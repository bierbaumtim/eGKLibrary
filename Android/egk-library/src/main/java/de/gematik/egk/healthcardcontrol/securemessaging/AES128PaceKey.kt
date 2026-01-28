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

import de.gematik.egk.cardreaderproviderapi.command.APDU
import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.cardreaderproviderapi.command.ResponseType
import de.gematik.egk.healthcardcontrol.crypto.AES
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1TaggedObject
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DERTaggedObject

/**
 * Class representing an AES128 PACE key holding an encryption secret and a MAC secret.
 * Implements the SecureMessaging protocol for encrypting/decrypting APDU commands and responses.
 * 
 * @property enc The encryption key (16 bytes)
 * @property mac The MAC key (16 bytes)
 */
class AES128PaceKey(
    private var enc: ByteArray,
    internal var mac: ByteArray
) : SecureMessaging {
    
    /** Send Sequence Counter */
    private var secureMessagingSsc: ByteArray = ByteArray(BLOCK_SIZE)
    
    companion object {
        const val BLOCK_SIZE = 16
        /** ISO/IEC 7816-4 padding tag */
        const val PADDING_DELIMITER: Byte = 0x80.toByte()
        
        /**
         * Increment the Send Sequence Counter (SSC).
         * 
         * @param ssc The Send Sequence Counter to increment
         * @return The incremented Send Sequence Counter
         */
        fun incrementSsc(ssc: ByteArray): ByteArray {
            val result = ByteArray(ssc.size)
            for (index in ssc.size - 1 downTo 0) {
                val temp = ssc[index]
                if (temp == 0xFF.toByte()) {
                    result[index] = 0
                } else {
                    result[index] = (temp + 1).toByte()
                    // Copy remaining bytes
                    for (j in 0 until index) {
                        result[j] = ssc[j]
                    }
                    break
                }
            }
            return result
        }
    }
    
    /**
     * Check if a command is already encrypted (CLA indicates secure messaging).
     */
    private val CommandType.isPaceKeyEncrypted: Boolean
        get() = (cla.toInt() and 0x0F) == 0x0C
    
    override fun encrypt(command: CommandType): CommandType {
        secureMessagingSsc = incrementSsc(secureMessagingSsc)
        return encryptCommand(command, enc, mac, secureMessagingSsc)
    }
    
    override fun decrypt(response: ResponseType): ResponseType {
        // If response is an error (SW != 9000/61XX/62XX/63XX), return it as-is
        // These responses are not encrypted
        val sw1 = response.sw1.toInt() and 0xFF
        if (sw1 != 0x90 && sw1 != 0x61 && sw1 != 0x62 && sw1 != 0x63) {
            return response
        }
        
        val data = response.data
        if (data == null || data.size < 10) {
            // Check if this is a warning status with no data (e.g., 6300, 63CX)
            if (sw1 == 0x63 && (data == null || data.isEmpty())) {
                return response
            }
            throw SecureMessagingException.EncryptedResponseMalformed()
        }
        
        secureMessagingSsc = incrementSsc(secureMessagingSsc)
        return decryptResponse(response, enc, mac, secureMessagingSsc)
    }
    
    override fun invalidate() {
        enc.fill(0)
        mac.fill(0)
        secureMessagingSsc.fill(0)
    }
    
    /**
     * Encrypt an APDU command according to the PACE secure messaging protocol.
     */
    private fun encryptCommand(
        command: CommandType,
        enc: ByteArray,
        mac: ByteArray,
        ssc: ByteArray
    ): CommandType {
        if (command.isPaceKeyEncrypted) {
            throw SecureMessagingException.ApduAlreadyEncrypted()
        }
        
        var header = byteArrayOf(command.cla.toByte(), command.ins.toByte(), command.p1.toByte(), command.p2.toByte())
        
        // Build encrypted data object (DO87)
        val dataObject: ByteArray = command.data?.let { data ->
            // gemSpec_COS#N032.300
            // Use CBC mode here instead of specified ECB mode, since only one block has to be encrypted
            val initVector = AES.CBC128.encrypt(ssc, enc)
            val paddedData = data.addPadding()
            val paddedDataEncrypted = AES.CBC128.encrypt(paddedData, enc, initVector)
            createTaggedObject(0x87, byteArrayOf(0x01) + paddedDataEncrypted)
        } ?: ByteArray(0)
        
        // Build length object (DO97)
        val lengthObject: ByteArray = command.ne?.let { ne ->
            val leValue: ByteArray = when {
                ne == 0x100 -> byteArrayOf(0x00)
                ne == 0x10000 -> byteArrayOf(0x00, 0x00)
                ne > 0x100 -> byteArrayOf(
                    ((ne shr 8) and 0xFF).toByte(),
                    (ne and 0xFF).toByte()
                )
                else -> byteArrayOf((ne and 0xFF).toByte())
            }
            createTaggedObject(0x97, leValue)
        } ?: ByteArray(0)
        
        // [REQ:gemSpec_COS:N032.500] Indicate Secure Messaging (Caution: we assume CLA in [0,3]!)
        header[0] = (header[0].toInt() or 0x0C).toByte()
        
        // [REQ:gemSpec_COS:N032.800] Calculate MAC
        val tmpData = dataObject + lengthObject
        val calculatedMac: ByteArray = if (tmpData.isEmpty()) {
            calculateMac(mac, ssc, header)
        } else {
            val padHeader = header.addPadding()
            calculateMac(mac, ssc, padHeader + tmpData)
        }
        
        // [REQ:gemSpec_COS:N032.900] Build APDU
        val mDo = createTaggedObject(0x8E, calculatedMac)
        
        // [REQ:gemSpec_COS:N033.000]
        val newData = tmpData + mDo
        
        // [REQ:gemSpec_COS:N033.100,N033.200,N033.300,N033.400]
        val setLe = when {
            command.data == null && command.ne == null -> APDU.EXPECTED_LENGTH_WILDCARD_SHORT
            command.data == null -> APDU.EXPECTED_LENGTH_WILDCARD_EXTENDED
            command.ne == null -> {
                if (newData.size <= 255) APDU.EXPECTED_LENGTH_WILDCARD_SHORT
                else APDU.EXPECTED_LENGTH_WILDCARD_EXTENDED
            }
            else -> APDU.EXPECTED_LENGTH_WILDCARD_EXTENDED
        }
        
        return APDU.Command.create(
            cla = header[0].toUByte(),
            ins = header[1].toUByte(),
            p1 = header[2].toUByte(),
            p2 = header[3].toUByte(),
            data = newData,
            ne = setLe
        )
    }
    
    /**
     * Parse BER-TLV encoded data and return all TLV objects with their encoded form.
     */
    private fun parseTlvObjects(data: ByteArray): Map<Int, Pair<ByteArray, ByteArray>> {
        val result = mutableMapOf<Int, Pair<ByteArray, ByteArray>>()
        var offset = 0
        
        while (offset < data.size) {
            if (offset + 2 > data.size) break
            
            val tlvStart = offset
            val tag = data[offset].toInt() and 0xFF
            offset++
            
            // Parse length
            val firstLengthByte = data[offset].toInt() and 0xFF
            offset++
            
            val length = when {
                firstLengthByte <= 127 -> firstLengthByte
                firstLengthByte == 0x81 -> {
                    val len = data[offset].toInt() and 0xFF
                    offset++
                    len
                }
                firstLengthByte == 0x82 -> {
                    val len = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
                    offset += 2
                    len
                }
                else -> throw SecureMessagingException.EncryptedResponseMalformed()
            }
            
            if (offset + length > data.size) {
                throw SecureMessagingException.EncryptedResponseMalformed()
            }
            
            val value = data.copyOfRange(offset, offset + length)
            val encoded = data.copyOfRange(tlvStart, offset + length)
            result[tag] = Pair(value, encoded)
            offset += length
        }
        
        return result
    }
    
    /**
     * Decrypt an APDU response according to the PACE secure messaging protocol.
     */
    private fun decryptResponse(
        response: ResponseType,
        enc: ByteArray,
        mac: ByteArray,
        ssc: ByteArray
    ): ResponseType {
        /**
         * Read APDU structure - gemSpec_COS#13.3
         * Case 1: DO99|DO8E|SW1SW2
         * Case 2: DO87|DO99|DO8E|SW1SW2
         * Case 3: DO99|DO8E|SW1SW2
         * Case 4: DO87|DO99|DO8E|SW1SW2
         */
        val responseData = response.data
            ?: throw SecureMessagingException.EncryptedResponseMalformed()
        
        // Parse all TLV objects in response
        val tlvObjects = parseTlvObjects(responseData)
        
        // MAC (tag 0x8E) is required
        val macEntry = tlvObjects[0x8E]
            ?: throw SecureMessagingException.EncryptedResponseMalformed()
        val macBytes = macEntry.first
        
        if (macBytes.size != 8) {
            throw SecureMessagingException.EncryptedResponseMalformed()
        }
        
        // Calculate MAC for verification (all data except MAC tag itself)
        // Reconstruct protected data from parsed TLVs (data + status, but not MAC)
        var protectedData = ByteArray(0)
        
        // Add data tag if present (DO87 or DO81)
        tlvObjects[0x87]?.second?.let { protectedData += it }
        tlvObjects[0x81]?.second?.let { protectedData += it }
        // Add status tag (DO99)
        tlvObjects[0x99]?.second?.let { protectedData += it }
        
        val calculatedMac = calculateMac(mac, ssc, protectedData)
        if (!macBytes.contentEquals(calculatedMac)) {
            throw SecureMessagingException.MacVerificationFailed()
        }
        
        // Processing status (tag 0x99) is required
        val statusBytes = tlvObjects[0x99]?.first
            ?: throw SecureMessagingException.EncryptedResponseMalformed()
        
        if (statusBytes.size != 2) {
            throw SecureMessagingException.EncryptedResponseMalformed()
        }
        
        // Check for data (tag 0x87 = encrypted, tag 0x81 = unencrypted)
        return when {
            tlvObjects.containsKey(0x87) -> {
                // Encrypted data - N033.800
                val encryptedData = tlvObjects[0x87]!!.first
                val initVector = AES.CBC128.encrypt(ssc, enc)
                // Skip first byte (padding indicator 0x01)
                val encryptedContent = encryptedData.copyOfRange(1, encryptedData.size)
                val paddedDecryptedData = AES.CBC128.decrypt(encryptedContent, enc, initVector)
                val decryptedData = paddedDecryptedData.removePadding()
                APDU.Response.create(decryptedData + statusBytes)
            }
            tlvObjects.containsKey(0x81) -> {
                // Data not encrypted - N033.600
                val plainData = tlvObjects[0x81]!!.first
                APDU.Response.create(plainData + statusBytes)
            }
            else -> {
                // No data, just status
                APDU.Response.create(statusBytes)
            }
        }
    }
    
    /**
     * Calculate MAC using AES-CMAC.
     */
    private fun calculateMac(key: ByteArray, ssc: ByteArray, macIn: ByteArray): ByteArray {
        val sscNormalized = ssc.normalize(BLOCK_SIZE, 0x00)
        val macInPadded = macIn.addPadding()
        val cmac = AES.cmac(key, sscNormalized + macInPadded)
        return cmac.copyOf(8)
    }
    
    /**
     * Create a BER-TLV tagged object for ISO 7816 secure messaging.
     * Creates simple TLV structure: tag | length | value
     */
    private fun createTaggedObject(tag: Int, data: ByteArray): ByteArray {
        // For ISO 7816 secure messaging, we need simple BER-TLV encoding
        // Tag is single byte (0x87, 0x97, 0x8E, etc.)
        // Length encoding:
        //   - If length <= 127: single byte
        //   - If length > 127: first byte = 0x80 + number of length bytes, then length bytes
        
        val lengthBytes = when {
            data.size <= 127 -> byteArrayOf(data.size.toByte())
            data.size <= 255 -> byteArrayOf(0x81.toByte(), data.size.toByte())
            data.size <= 65535 -> byteArrayOf(
                0x82.toByte(),
                ((data.size shr 8) and 0xFF).toByte(),
                (data.size and 0xFF).toByte()
            )
            else -> throw IllegalArgumentException("Data too large for BER-TLV encoding: ${data.size}")
        }
        
        return byteArrayOf(tag.toByte()) + lengthBytes + data
    }
    
    /**
     * Parse a tagged object and return the tag number and content.
     */
    private fun parseTaggedObject(data: ByteArray): Pair<Int, ByteArray> {
        return try {
            ASN1InputStream(data).use { asn1Stream ->
                val obj = asn1Stream.readObject()
                when (obj) {
                    is ASN1TaggedObject -> {
                        val content = obj.baseObject
                        val bytes = when (content) {
                            is ASN1OctetString -> content.octets
                            is ASN1Primitive -> content.encoded.let { enc ->
                                // Skip tag and length bytes
                                if (enc.size > 2) enc.copyOfRange(2, enc.size) else enc
                            }
                            else -> (content as? ASN1OctetString)?.octets ?: ByteArray(0)
                        }
                        Pair(obj.tagNo, bytes)
                    }
                    else -> throw SecureMessagingException.EncryptedResponseMalformed()
                }
            }
        } catch (e: Exception) {
            if (e is SecureMessagingException) throw e
            throw SecureMessagingException.EncryptedResponseMalformed()
        }
    }
}

/**
 * Add ISO/IEC 7816-4 padding to data.
 */
internal fun ByteArray.addPadding(
    delimiter: Byte = AES128PaceKey.PADDING_DELIMITER,
    blockSize: Int = AES128PaceKey.BLOCK_SIZE
): ByteArray {
    val paddingLength = blockSize - (this.size % blockSize)
    val padded = ByteArray(this.size + paddingLength)
    System.arraycopy(this, 0, padded, 0, this.size)
    padded[this.size] = delimiter
    // Remaining bytes are already 0x00
    return padded
}

/**
 * Remove ISO/IEC 7816-4 padding from data.
 */
internal fun ByteArray.removePadding(
    delimiter: Byte = AES128PaceKey.PADDING_DELIMITER
): ByteArray {
    val lastIndex = this.lastIndexOf(delimiter)
    return if (lastIndex >= 0) {
        this.copyOfRange(0, lastIndex)
    } else {
        ByteArray(0)
    }
}

/**
 * Normalize data to a specific size with padding.
 */
internal fun ByteArray.normalize(size: Int, paddingByte: Byte): ByteArray {
    return if (this.size >= size) {
        this.copyOf(size)
    } else {
        val result = ByteArray(size)
        result.fill(paddingByte)
        System.arraycopy(this, 0, result, size - this.size, this.size)
        result
    }
}
