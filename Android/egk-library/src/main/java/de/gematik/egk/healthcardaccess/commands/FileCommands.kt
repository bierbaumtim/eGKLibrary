/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */

package de.gematik.egk.healthcardaccess.commands

import de.gematik.egk.cardreaderproviderapi.command.APDU
import de.gematik.egk.healthcardaccess.cardobjects.ApplicationIdentifier
import de.gematik.egk.healthcardaccess.cardobjects.FileIdentifier
import de.gematik.egk.healthcardaccess.cardobjects.ShortFileIdentifier
import de.gematik.egk.healthcardaccess.responses.ResponseStatus

/**
 * Commands for selecting and accessing files on the health card
 */
object SelectCommands {
    private const val CLA: UByte = 0x00u
    private const val INS: UByte = 0xA4u

    private val responseMessages: Map<UShort, ResponseStatus> = mapOf(
        ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
        ResponseStatus.FILE_DEACTIVATED.code to ResponseStatus.FILE_DEACTIVATED,
        ResponseStatus.FILE_TERMINATED.code to ResponseStatus.FILE_TERMINATED,
        ResponseStatus.FILE_NOT_FOUND.code to ResponseStatus.FILE_NOT_FOUND,
        ResponseStatus.WRONG_FILE_TYPE.code to ResponseStatus.WRONG_FILE_TYPE
    )

    /**
     * Select a DF/MF by AID
     *
     * @param aid the ApplicationIdentifier to select
     * @param selectDfElseAid true to select by DF name, false to select by AID
     * @param requestFcp true to request FCP data in response
     * @param selectNextOccurrence true to select next occurrence
     * @return the select command
     */
    fun selectFile(
        aid: ApplicationIdentifier,
        selectDfElseAid: Boolean = false,
        requestFcp: Boolean = true,
        selectNextOccurrence: Boolean = false
    ): HealthCardCommand {
        val p1: UByte = if (selectDfElseAid) 0x01u else 0x04u
        val p2: UByte = when {
            selectNextOccurrence && requestFcp -> 0x06u
            selectNextOccurrence -> 0x02u
            requestFcp -> 0x04u
            else -> 0x0Cu
        }

        return HealthCardCommandBuilder()
            .setCla(CLA)
            .setIns(INS)
            .setP1(p1)
            .setP2(p2)
            .setData(aid.rawValue)
            .setNe(if (requestFcp) APDU.EXPECTED_LENGTH_WILDCARD_SHORT else null)
            .setResponseStatuses(responseMessages)
            .build()
    }

    /**
     * Select a child DF by FID
     *
     * @param fid the FileIdentifier to select
     * @param requestFcp true to request FCP data in response
     * @return the select command
     */
    fun selectChildDf(
        fid: FileIdentifier,
        requestFcp: Boolean = true
    ): HealthCardCommand {
        val p2: UByte = if (requestFcp) 0x04u else 0x0Cu

        return HealthCardCommandBuilder()
            .setCla(CLA)
            .setIns(INS)
            .setP1(0x01u)
            .setP2(p2)
            .setData(fid.rawValue)
            .setNe(if (requestFcp) APDU.EXPECTED_LENGTH_WILDCARD_SHORT else null)
            .setResponseStatuses(responseMessages)
            .build()
    }

    /**
     * Select parent DF
     *
     * @param requestFcp true to request FCP data in response
     * @return the select command
     */
    fun selectParentDf(
        requestFcp: Boolean = true
    ): HealthCardCommand {
        val p2: UByte = if (requestFcp) 0x04u else 0x0Cu

        return HealthCardCommandBuilder()
            .setCla(CLA)
            .setIns(INS)
            .setP1(0x03u)
            .setP2(p2)
            .setNe(if (requestFcp) APDU.EXPECTED_LENGTH_WILDCARD_SHORT else null)
            .setResponseStatuses(responseMessages)
            .build()
    }

    /**
     * Select EF by FID
     *
     * @param fid the FileIdentifier to select
     * @param requestFcp true to request FCP data in response
     * @return the select command
     */
    fun selectEf(
        fid: FileIdentifier,
        requestFcp: Boolean = true
    ): HealthCardCommand {
        val p2: UByte = if (requestFcp) 0x04u else 0x0Cu

        return HealthCardCommandBuilder()
            .setCla(CLA)
            .setIns(INS)
            .setP1(0x02u)
            .setP2(p2)
            .setData(fid.rawValue)
            .setNe(if (requestFcp) APDU.EXPECTED_LENGTH_WILDCARD_SHORT else null)
            .setResponseStatuses(responseMessages)
            .build()
    }
}

/**
 * Commands for reading data from the health card
 */
object ReadCommands {

    private val readResponseMessages: Map<UShort, ResponseStatus> = mapOf(
        ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
        ResponseStatus.END_OF_FILE_WARNING.code to ResponseStatus.END_OF_FILE_WARNING,
        ResponseStatus.SECURITY_STATUS_NOT_SATISFIED.code to ResponseStatus.SECURITY_STATUS_NOT_SATISFIED,
        ResponseStatus.NO_CURRENT_EF.code to ResponseStatus.NO_CURRENT_EF,
        ResponseStatus.OFFSET_TOO_BIG.code to ResponseStatus.OFFSET_TOO_BIG
    )

    /**
     * Read Binary command with offset and short file identifier
     *
     * @param sfid optional short file identifier (null uses current EF)
     * @param offset the offset to start reading from
     * @param ne expected response length
     * @return the read binary command
     */
    fun readBinary(
        sfid: ShortFileIdentifier? = null,
        offset: Int,
        ne: Int = APDU.EXPECTED_LENGTH_WILDCARD_SHORT
    ): HealthCardCommand {
        val p1: UByte = if (sfid != null) {
            // Set bit 8 and include SFID in bits 1-5
            (0x80u or sfid.rawValue[0].toUInt()).toUByte()
        } else {
            // High byte of offset
            ((offset shr 8) and 0xFF).toUByte()
        }

        val p2: UByte = if (sfid != null) {
            // Low byte of offset
            (offset and 0xFF).toUByte()
        } else {
            // Low byte of offset
            (offset and 0xFF).toUByte()
        }

        return HealthCardCommandBuilder()
            .setCla(0x00u)
            .setIns(0xB0u)
            .setP1(p1)
            .setP2(p2)
            .setNe(ne)
            .setResponseStatuses(readResponseMessages)
            .build()
    }

    /**
     * Read Binary with extended length and 3-byte offset
     * For files larger than 32KB
     *
     * @param offset the offset as a 3-byte value
     * @param ne expected response length
     * @return the read binary command
     */
    fun readBinaryExtended(
        offset: Int,
        ne: Int = APDU.EXPECTED_LENGTH_WILDCARD_EXTENDED
    ): HealthCardCommand {
        val offsetData = byteArrayOf(
            0x54, 0x03, // Tag and length for offset DO
            ((offset shr 16) and 0xFF).toByte(),
            ((offset shr 8) and 0xFF).toByte(),
            (offset and 0xFF).toByte()
        )

        return HealthCardCommandBuilder()
            .setCla(0x00u)
            .setIns(0xB1u)
            .setP1(0x00u)
            .setP2(0x00u)
            .setData(offsetData)
            .setNe(ne)
            .setResponseStatuses(readResponseMessages)
            .build()
    }
    
    /**
     * Read file command - reads from the currently selected file.
     * 
     * @param ne Expected number of bytes to read
     * @param offset Starting offset within the file
     * @return The read command
     */
    fun readFile(
        ne: Int,
        offset: Int = 0
    ): HealthCardCommand {
        return readBinary(sfid = null, offset = offset, ne = ne)
    }
    
    /**
     * Read file using short file identifier.
     * 
     * @param sfid The short file identifier
     * @param ne Expected number of bytes to read
     * @param offset Starting offset within the file
     * @return The read command
     */
    fun readFileWithSfid(
        sfid: ShortFileIdentifier,
        ne: Int,
        offset: Int = 0
    ): HealthCardCommand {
        return readBinary(sfid = sfid, offset = offset, ne = ne)
    }
}

/**
 * Commands for Manage Security Environment
 */
object ManageSecurityEnvironmentCommands {
    private const val CLA: UByte = 0x00u
    private const val INS: UByte = 0x22u

    private val responseMessages: Map<UShort, ResponseStatus> = mapOf(
        ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
        ResponseStatus.UNSUPPORTED_FUNCTION.code to ResponseStatus.UNSUPPORTED_FUNCTION,
        ResponseStatus.KEY_NOT_FOUND.code to ResponseStatus.KEY_NOT_FOUND
    )

    /**
     * MSE: SET for PACE key agreement
     *
     * @param oid the OID for the PACE algorithm
     * @param keyRef the key reference (usually 2 for CAN)
     * @return the MSE:SET command
     */
    fun setForPace(
        oid: ByteArray,
        keyRef: Int = 2
    ): HealthCardCommand {
        // Build data: OID tag (0x80) + Key Reference tag (0x83)
        val data = byteArrayOf(
            0x80.toByte(), oid.size.toByte()
        ) + oid + byteArrayOf(
            0x83.toByte(), 0x01, keyRef.toByte()
        )

        return HealthCardCommandBuilder()
            .setCla(CLA)
            .setIns(INS)
            .setP1(0xC1u) // SET
            .setP2(0xA4u) // Authentication (AT)
            .setData(data)
            .setResponseStatuses(responseMessages)
            .build()
    }
    
    /**
     * MSE: SELECT PACE - Selects the PACE protocol for key agreement
     * 
     * @param symmetricKey The symmetric key to use (contains key reference)
     * @param dfSpecific Whether the key is DF-specific
     * @param oid The OID string for the PACE algorithm (e.g., "0.4.0.127.0.7.2.2.4.2.2")
     * @return The MSE:SET command for PACE selection
     */
    fun selectPACE(
        symmetricKey: de.gematik.egk.healthcardaccess.cardobjects.Key,
        dfSpecific: Boolean,
        oid: String
    ): HealthCardCommand {
        // Convert OID string to bytes
        val oidBytes = oidStringToBytes(oid)
        
        // Calculate key reference: if dfSpecific, set bit 7
        val keyRef = if (dfSpecific) {
            (symmetricKey.keyId.toInt() or 0x80).toByte()
        } else {
            symmetricKey.keyId.toByte()
        }
        
        // Build data: OID tag (0x80) + Key Reference tag (0x83)
        val data = byteArrayOf(
            0x80.toByte(), oidBytes.size.toByte()
        ) + oidBytes + byteArrayOf(
            0x83.toByte(), 0x01.toByte(), keyRef
        )

        return HealthCardCommandBuilder()
            .setCla(CLA)
            .setIns(INS)
            .setP1(0xC1u) // SET
            .setP2(0xA4u) // Authentication (AT)
            .setData(data)
            .setResponseStatuses(responseMessages)
            .build()
    }
    
    /**
     * Convert an OID string (e.g., "0.4.0.127.0.7.2.2.4.2.2") to ASN.1 OID bytes.
     */
    private fun oidStringToBytes(oid: String): ByteArray {
        val components = oid.split(".").map { it.toInt() }
        require(components.size >= 2) { "OID must have at least 2 components" }
        
        val bytes = mutableListOf<Byte>()
        
        // First two components are encoded as: 40 * first + second
        bytes.add((40 * components[0] + components[1]).toByte())
        
        // Remaining components use variable-length encoding
        for (i in 2 until components.size) {
            val value = components[i]
            if (value < 128) {
                bytes.add(value.toByte())
            } else {
                // Multi-byte encoding for values >= 128
                val encoded = mutableListOf<Byte>()
                var v = value
                encoded.add((v and 0x7F).toByte())
                v = v shr 7
                while (v > 0) {
                    encoded.add(((v and 0x7F) or 0x80).toByte())
                    v = v shr 7
                }
                encoded.reverse()
                bytes.addAll(encoded)
            }
        }
        
        return bytes.toByteArray()
    }

    /**
     * MSE: SET for internal authentication
     *
     * @param keyRef the key reference
     * @param dfSpecific whether key is DF-specific
     * @param algorithmId optional algorithm identifier
     * @return the MSE:SET command
     */
    fun setForInternalAuth(
        keyRef: UByte,
        dfSpecific: Boolean = false,
        algorithmId: ByteArray? = null
    ): HealthCardCommand {
        val actualKeyRef = if (dfSpecific) (keyRef + 0x80u).toUByte() else keyRef

        val dataBuilder = mutableListOf<Byte>()

        if (algorithmId != null) {
            dataBuilder.add(0x80.toByte())
            dataBuilder.add(algorithmId.size.toByte())
            dataBuilder.addAll(algorithmId.toList())
        }

        dataBuilder.add(0x84.toByte())
        dataBuilder.add(0x01)
        dataBuilder.add(actualKeyRef.toByte())

        return HealthCardCommandBuilder()
            .setCla(CLA)
            .setIns(INS)
            .setP1(0x41u) // SET
            .setP2(0xA4u) // Authentication
            .setData(dataBuilder.toByteArray())
            .setResponseStatuses(responseMessages)
            .build()
    }
}
