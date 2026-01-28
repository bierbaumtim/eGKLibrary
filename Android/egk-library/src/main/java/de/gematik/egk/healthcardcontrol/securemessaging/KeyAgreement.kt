// Copyright (c) 2024 gematik GmbH
// 
// Licensed under the Apache License, Version 2.0 (the License);
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an AS IS BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.gematik.egk.healthcardcontrol.securemessaging

import de.gematik.egk.healthcardaccess.HealthCardType
import de.gematik.egk.healthcardaccess.cardobjects.Key
import de.gematik.egk.healthcardaccess.commands.HealthCardCommand
import de.gematik.egk.healthcardcontrol.crypto.AES
import de.gematik.egk.healthcardcontrol.crypto.BrainpoolP256r1
import de.gematik.egk.healthcardcontrol.crypto.KeyDerivationFunction
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1TaggedObject
import org.bouncycastle.asn1.DERApplicationSpecific
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.DERTaggedObject
import java.security.interfaces.ECPublicKey

/**
 * Holds functionality to negotiate a common key with a given HealthCard and a CardAccessNumber.
 */
object KeyAgreement {
    
    /**
     * Errors that can occur during key agreement.
     */
    sealed class Error : Exception() {
        object IllegalArgument : Error()
        object UnexpectedFormedAnswerFromCard : Error()
        object ResultOfEcArithmeticWasInfinite : Error()
        object MacPcdVerificationFailedOnCard : Error()
        object MacPiccVerificationFailedLocally : Error()
        object NoValidHealthCardStatus : Error()
        object EfCardAccessNotAvailable : Error()
        data class UnsupportedKeyAgreementAlgorithm(val oid: String) : Error()
    }
    
    /**
     * Algorithm the PACE key agreement negotiation is based on.
     */
    enum class Algorithm(
        val protocolIdentifierHex: String,
        val protocolIdentifier: String,
        val affectedKeyId: UByte,
        val macTokenPrefixSize: Int
    ) {
        /** id-PACE-ECDH-GM-AES-CBC-CMAC-128 */
        ID_PACE_ECDH_GM_AES_CBC_CMAC_128(
            protocolIdentifierHex = "060A04007F00070202040202",
            protocolIdentifier = "0.4.0.127.0.7.2.2.4.2.2",
            affectedKeyId = 0x02u,
            macTokenPrefixSize = 8
        )
    }
    
    /**
     * Negotiate a common key with a HealthCard given its CardAccessNumber.
     * 
     * @param card The card to negotiate a session key with
     * @param can The Card Access Number of the HealthCard
     * @param algorithm The PACE algorithm to use (default: ID_PACE_ECDH_GM_AES_CBC_CMAC_128)
     * @return SecureMessaging instance employing the PACE key that both this application and the card agreed on
     */
    suspend fun negotiateSessionKey(
        card: HealthCardType,
        can: String,
        algorithm: Algorithm = Algorithm.ID_PACE_ECDH_GM_AES_CBC_CMAC_128
    ): SecureMessaging {
        return when (algorithm) {
            Algorithm.ID_PACE_ECDH_GM_AES_CBC_CMAC_128 -> {
                // Step 0: Set security environment
                step0PaceEcdhGmAesCbcCmac128(card, algorithm)
                
                // Step 1: Request nonceZ from card and decrypt it to nonceS
                val nonceS = step1PaceEcdhGmAesCbcCmac128(card, can)
                
                // Step 2: Generate first key pair, send PK1_PCD, receive PK1_PICC
                // Calculate gTilde and generate PK2_PCD
                val (pk2Pcd, keyPair2) = step2PaceEcdhGmAesCbcCmac128(card, nonceS)
                
                // Step 3: Send PK2_PCD, receive PK2_PICC, derive PACE key
                val (pk2Picc, paceKey) = step3PaceEcdhGmAesCbcCmac128(card, pk2Pcd, keyPair2)
                
                // Step 4: Verify MAC tokens
                val verifyMacPicc = step4PaceEcdhGmAesCbcCmac128(
                    card, pk2Picc, pk2Pcd, paceKey, algorithm
                )
                
                if (!verifyMacPicc) {
                    throw Error.MacPiccVerificationFailedLocally
                }
                
                paceKey
            }
        }
    }
    
    /**
     * Step 0: Set the appropriate security environment on card.
     */
    private suspend fun step0PaceEcdhGmAesCbcCmac128(
        card: HealthCardType,
        algorithm: Algorithm
    ) {
        val key = Key(algorithm.affectedKeyId)
        val oid = algorithm.protocolIdentifier
        
        val selectPaceCommand = HealthCardCommand.ManageSE.selectPACE(
            symmetricKey = key,
            dfSpecific = false,
            oid = oid
        )
        
        card.transmit(selectPaceCommand)
    }
    
    /**
     * Step 1: Request nonceZ from card and decrypt it to nonceS.
     */
    private suspend fun step1PaceEcdhGmAesCbcCmac128(
        card: HealthCardType,
        can: String
    ): ByteArray {
        val paceStep1aCommand = HealthCardCommand.PACE.step1a()
        val paceStep1aResponse = card.transmit(paceStep1aCommand)
        
        val responseData = paceStep1aResponse.data
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val nonceZ = extractPrimitive(responseData)
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val derivedKey = KeyDerivationFunction.deriveKey(can, KeyDerivationFunction.Mode.PASSWORD)
        return AES.CBC128.decrypt(nonceZ, derivedKey)
    }
    
    /**
     * Step 2: Generate first key pair, send PK1_PCD, receive PK1_PICC.
     * Calculate shared secret generating point gTilde.
     * Generate second key pair and PK2_PCD = gTilde * keyPair2.privateKey
     */
    private suspend fun step2PaceEcdhGmAesCbcCmac128(
        card: HealthCardType,
        nonceS: ByteArray
    ): Pair<ECPublicKey, BrainpoolP256r1.KeyExchangeKeyPair> {
        val keyPair1 = BrainpoolP256r1.generateKeyPair()
        
        val paceStep2aCommand = HealthCardCommand.PACE.step2a(keyPair1.publicKeyX962())
        val pk1PiccResponse = card.transmit(paceStep2aCommand)
        
        val pk1PiccResponseData = pk1PiccResponse.data
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val pk1PiccData = extractPrimitive(pk1PiccResponseData)
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val pk1Picc = BrainpoolP256r1.publicKeyFromX962(pk1PiccData)
        
        return keyPair1.paceMapNonce(nonceS, pk1Picc)
    }
    
    /**
     * Step 3: Send PK2_PCD to card, receive PK2_PICC.
     * Derive PACE key from all the information.
     */
    private suspend fun step3PaceEcdhGmAesCbcCmac128(
        card: HealthCardType,
        pk2Pcd: ECPublicKey,
        keyPair2: BrainpoolP256r1.KeyExchangeKeyPair
    ): Pair<ECPublicKey, AES128PaceKey> {
        val pk2PcdX962 = ecPublicKeyToX962(pk2Pcd)
        val paceStep3Command = HealthCardCommand.PACE.step3a(pk2PcdX962)
        val pk2PiccResponse = card.transmit(paceStep3Command)
        
        val pk2PiccResponseData = pk2PiccResponse.data
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val pk2PiccData = extractPrimitive(pk2PiccResponseData)
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val pk2Picc = BrainpoolP256r1.publicKeyFromX962(pk2PiccData)
        val paceKeyData = BrainpoolP256r1.derivePaceKey(pk2Picc, keyPair2)
        val paceKey = AES128PaceKey(paceKeyData.enc, paceKeyData.mac)
        
        return Pair(pk2Picc, paceKey)
    }
    
    /**
     * Step 4: Derive MAC_PCD and send to card for verification.
     * Receive MAC_PICC from card and verify it locally.
     */
    private suspend fun step4PaceEcdhGmAesCbcCmac128(
        card: HealthCardType,
        pk2Picc: ECPublicKey,
        pk2Pcd: ECPublicKey,
        paceKey: AES128PaceKey,
        algorithm: Algorithm
    ): Boolean {
        val pk2PiccX962 = ecPublicKeyToX962(pk2Picc)
        val pk2PcdX962 = ecPublicKeyToX962(pk2Pcd)
        
        val macPcd = deriveMac(pk2PiccX962, paceKey.mac, algorithm)
        val macPcdToken = macPcd.copyOf(algorithm.macTokenPrefixSize)
        
        val paceStep4aCommand = HealthCardCommand.PACE.step4a(macPcdToken)
        val macPiccResponse = card.transmit(paceStep4aCommand)
        
        if (macPiccResponse.responseStatus != de.gematik.egk.healthcardaccess.responses.ResponseStatus.SUCCESS) {
            throw Error.MacPcdVerificationFailedOnCard
        }
        
        val macPiccResponseData = macPiccResponse.data
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val macPiccData = extractPrimitive(macPiccResponseData)
            ?: throw Error.UnexpectedFormedAnswerFromCard
        
        val verifyMacPiccData = deriveMac(pk2PcdX962, paceKey.mac, algorithm)
        
        return macPiccData.contentEquals(verifyMacPiccData.copyOf(8))
    }
    
    /**
     * Extract primitive data from ASN.1 constructed object.
     */
    private fun extractPrimitive(constructedAsn1: ByteArray): ByteArray? {
        return try {
            ASN1InputStream(constructedAsn1).use { stream ->
                val obj = stream.readObject()
                when (obj) {
                    is ASN1Sequence -> {
                        val firstElement = obj.getObjectAt(0)
                        when (firstElement) {
                            is ASN1TaggedObject -> {
                                val primitive = firstElement.baseObject
                                when (primitive) {
                                    is DEROctetString -> primitive.octets
                                    else -> null
                                }
                            }
                            is DEROctetString -> firstElement.octets
                            else -> null
                        }
                    }
                    is ASN1TaggedObject -> {
                        val primitive = obj.baseObject
                        when (primitive) {
                            is DEROctetString -> primitive.octets
                            else -> null
                        }
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Derive MAC from public key and session key.
     */
    private fun deriveMac(publicKeyX962: ByteArray, sessionKeyMac: ByteArray, algorithm: Algorithm): ByteArray {
        val asn1AuthToken = createAsn1AuthToken(publicKeyX962, algorithm.protocolIdentifier)
        return AES.cmac(sessionKeyMac, asn1AuthToken)
    }
    
    /**
     * Create ASN.1 authentication token for MAC derivation.
     * Structure: [0x7F49] { [0x06] OID, [0x86] publicKey }
     */
    private fun createAsn1AuthToken(publicKeyX962: ByteArray, protocolId: String): ByteArray {
        // Create OID
        val oid = ASN1ObjectIdentifier(protocolId)
        val taggedOid = DERTaggedObject(false, 0x06, oid)
        
        // Create public key tagged object
        val publicKeyOctet = DEROctetString(publicKeyX962)
        val taggedPublicKey = DERTaggedObject(false, 0x06, publicKeyOctet)
        
        // Create sequence
        val sequence = DERSequence(arrayOf(taggedOid, taggedPublicKey))
        
        // Wrap in application tag 0x49 (0x7F49)
        val appSpecific = DERApplicationSpecific(0x49, sequence)
        
        return appSpecific.encoded
    }
    
    /**
     * Convert ECPublicKey to X9.62 uncompressed point format.
     */
    private fun ecPublicKeyToX962(publicKey: ECPublicKey): ByteArray {
        val x = publicKey.w.affineX.toByteArray().let { bytes ->
            when {
                bytes.size == 32 -> bytes
                bytes.size > 32 -> bytes.copyOfRange(bytes.size - 32, bytes.size)
                else -> ByteArray(32 - bytes.size) + bytes
            }
        }
        val y = publicKey.w.affineY.toByteArray().let { bytes ->
            when {
                bytes.size == 32 -> bytes
                bytes.size > 32 -> bytes.copyOfRange(bytes.size - 32, bytes.size)
                else -> ByteArray(32 - bytes.size) + bytes
            }
        }
        return byteArrayOf(0x04) + x + y
    }
}
