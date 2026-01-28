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

package de.gematik.egk.healthcardcontrol.crypto

import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import javax.crypto.KeyAgreement

/**
 * Elliptic Curve operations using Brainpool P256r1 curve for PACE protocol.
 */
object BrainpoolP256r1 {
    
    private const val CURVE_NAME = "brainpoolP256r1"
    
    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
    
    private val ecParams: X9ECParameters = CustomNamedCurves.getByName(CURVE_NAME)
    
    private val domainParams = ECDomainParameters(
        ecParams.curve,
        ecParams.g,
        ecParams.n,
        ecParams.h
    )
    
    private val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
    
    /**
     * Represents an EC key pair for key exchange operations.
     */
    class KeyExchangeKeyPair(
        val privateKey: ECPrivateKey,
        val publicKey: ECPublicKey
    ) {
        /**
         * Get the public key in X9.62 uncompressed point format.
         */
        fun publicKeyX962(): ByteArray {
            val bcSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
            val point = bcSpec.curve.createPoint(
                publicKey.w.affineX,
                publicKey.w.affineY
            )
            return point.getEncoded(false)
        }
        
        /**
         * Perform ECDH key agreement with the given peer public key.
         * 
         * @param peerPublicKey The peer's public key
         * @return The shared secret
         */
        fun sharedSecret(peerPublicKey: ECPublicKey): ByteArray {
            val keyAgreement = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(peerPublicKey, true)
            return keyAgreement.generateSecret()
        }
        
        /**
         * Perform PACE nonce mapping to create a new generator point.
         * 
         * This implements the Generic Mapping according to BSI TR-03110.
         * 
         * @param nonce The decrypted nonce from the card (nonceS)
         * @param peerKey1 The first public key received from the card (PK1_PICC)
         * @return Pair of new public key (PK2_PCD) and new key pair for final key derivation
         */
        fun paceMapNonce(nonce: ByteArray, peerKey1: ECPublicKey): Pair<ECPublicKey, KeyExchangeKeyPair> {
            // 1. Calculate shared secret H = keyPair1.privateKey * PK1_PICC
            val sharedSecretBytes = sharedSecret(peerKey1)
            
            // 2. Calculate new generator: gTilde = nonceS * G + H
            // Convert nonce to BigInteger
            val nonceS = BigInteger(1, nonce)
            
            // Get the curve parameters
            val curve = ecParams.curve
            val basePoint = ecParams.g
            
            // Convert shared secret to EC point (interpret as x-coordinate, derive y)
            // Actually, for PACE Generic Mapping, we need H as a point, not bytes
            // The shared secret from ECDH is the x-coordinate only
            // We need to reconstruct the full point
            
            // Create point from peer's public key coordinates
            val peerPoint = curve.createPoint(peerKey1.w.affineX, peerKey1.w.affineY)
            
            // Calculate H = privateKey * peerPoint (this is what ECDH computes internally)
            val bcPrivateKey = BigInteger(1, padTo32Bytes(privateKey.s.toByteArray()))
            val hPoint = peerPoint.multiply(bcPrivateKey).normalize()
            
            // Calculate nonceS * G
            val nonceSTimesG = basePoint.multiply(nonceS).normalize()
            
            // gTilde = nonceS * G + H
            val gTilde = nonceSTimesG.add(hPoint).normalize()
            
            if (gTilde.isInfinity) {
                throw IllegalStateException("Result of EC arithmetic was infinite")
            }
            
            // 3. Generate new key pair with gTilde as generator
            val keyPair2PrivateValue = generatePrivateKeyValue()
            
            // PK2_PCD = keyPair2.privateKey * gTilde
            val pk2PcdPoint = gTilde.multiply(keyPair2PrivateValue).normalize()
            
            // Convert to Java EC keys
            val pk2Pcd = pointToPublicKey(pk2PcdPoint)
            val keyPair2 = createKeyPairFromPrivateValue(keyPair2PrivateValue, gTilde)
            
            return Pair(pk2Pcd, keyPair2)
        }
    }
    
    /**
     * Generate a new EC key pair on the Brainpool P256r1 curve.
     */
    fun generateKeyPair(): KeyExchangeKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(ecSpec, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()
        return KeyExchangeKeyPair(
            keyPair.private as ECPrivateKey,
            keyPair.public as ECPublicKey
        )
    }
    
    /**
     * Create a public key from X9.62 encoded bytes.
     */
    fun publicKeyFromX962(data: ByteArray): ECPublicKey {
        val point = ecParams.curve.decodePoint(data)
        val javaSpec = java.security.spec.ECParameterSpec(
            java.security.spec.EllipticCurve(
                java.security.spec.ECFieldFp(ecParams.curve.field.characteristic),
                ecParams.curve.a.toBigInteger(),
                ecParams.curve.b.toBigInteger()
            ),
            java.security.spec.ECPoint(ecParams.g.xCoord.toBigInteger(), ecParams.g.yCoord.toBigInteger()),
            ecParams.n,
            ecParams.h.intValueExact()
        )
        val pubKeySpec = ECPublicKeySpec(
            java.security.spec.ECPoint(point.xCoord.toBigInteger(), point.yCoord.toBigInteger()),
            javaSpec
        )
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return keyFactory.generatePublic(pubKeySpec) as ECPublicKey
    }
    
    /**
     * Derive PACE key from shared secret.
     * 
     * @param publicKey The peer's final public key (PK2_PICC)
     * @param keyPair The final key pair for ECDH
     * @return The derived PACE key
     */
    fun derivePaceKey(publicKey: ECPublicKey, keyPair: KeyExchangeKeyPair): AES128PaceKeyData {
        val sharedSecret = keyPair.sharedSecret(publicKey)
        val keyEnc = KeyDerivationFunction.deriveKey(sharedSecret, KeyDerivationFunction.Mode.ENC)
        val keyMac = KeyDerivationFunction.deriveKey(sharedSecret, KeyDerivationFunction.Mode.MAC)
        return AES128PaceKeyData(keyEnc, keyMac)
    }
    
    // Private helper methods
    
    private fun generatePrivateKeyValue(): BigInteger {
        val random = SecureRandom()
        var privateValue: BigInteger
        do {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            privateValue = BigInteger(1, bytes)
        } while (privateValue >= ecParams.n || privateValue == BigInteger.ZERO)
        return privateValue
    }
    
    private fun pointToPublicKey(point: ECPoint): ECPublicKey {
        val javaSpec = java.security.spec.ECParameterSpec(
            java.security.spec.EllipticCurve(
                java.security.spec.ECFieldFp(ecParams.curve.field.characteristic),
                ecParams.curve.a.toBigInteger(),
                ecParams.curve.b.toBigInteger()
            ),
            java.security.spec.ECPoint(ecParams.g.xCoord.toBigInteger(), ecParams.g.yCoord.toBigInteger()),
            ecParams.n,
            ecParams.h.intValueExact()
        )
        val pubKeySpec = ECPublicKeySpec(
            java.security.spec.ECPoint(point.xCoord.toBigInteger(), point.yCoord.toBigInteger()),
            javaSpec
        )
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return keyFactory.generatePublic(pubKeySpec) as ECPublicKey
    }
    
    private fun createKeyPairFromPrivateValue(privateValue: BigInteger, generator: ECPoint): KeyExchangeKeyPair {
        val javaSpec = java.security.spec.ECParameterSpec(
            java.security.spec.EllipticCurve(
                java.security.spec.ECFieldFp(ecParams.curve.field.characteristic),
                ecParams.curve.a.toBigInteger(),
                ecParams.curve.b.toBigInteger()
            ),
            java.security.spec.ECPoint(generator.xCoord.toBigInteger(), generator.yCoord.toBigInteger()),
            ecParams.n,
            ecParams.h.intValueExact()
        )
        
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        
        // Private key
        val privKeySpec = ECPrivateKeySpec(privateValue, javaSpec)
        val privateKey = keyFactory.generatePrivate(privKeySpec) as ECPrivateKey
        
        // Public key = privateValue * generator
        val publicPoint = generator.multiply(privateValue).normalize()
        val pubKeySpec = ECPublicKeySpec(
            java.security.spec.ECPoint(publicPoint.xCoord.toBigInteger(), publicPoint.yCoord.toBigInteger()),
            javaSpec
        )
        val publicKey = keyFactory.generatePublic(pubKeySpec) as ECPublicKey
        
        return KeyExchangeKeyPair(privateKey, publicKey)
    }
    
    private fun padTo32Bytes(data: ByteArray): ByteArray {
        return when {
            data.size == 32 -> data
            data.size > 32 -> data.copyOfRange(data.size - 32, data.size)
            else -> ByteArray(32 - data.size) + data
        }
    }
}

/**
 * Data class holding the derived PACE keys.
 */
data class AES128PaceKeyData(
    val enc: ByteArray,
    val mac: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AES128PaceKeyData) return false
        return enc.contentEquals(other.enc) && mac.contentEquals(other.mac)
    }
    
    override fun hashCode(): Int {
        var result = enc.contentHashCode()
        result = 31 * result + mac.contentHashCode()
        return result
    }
}
