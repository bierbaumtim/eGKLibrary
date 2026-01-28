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
import de.gematik.egk.healthcardaccess.responses.ResponseStatus

/**
 * These builders represent the commands in gemSpec_COS#14.7 "Komponentenauthentisierung".
 */
object AuthenticationCommands {

    private val externalResponseMessages: Map<UShort, ResponseStatus> = mapOf(
        ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
        ResponseStatus.AUTHENTICATION_FAILURE.code to ResponseStatus.AUTHENTICATION_FAILURE,
        ResponseStatus.SECURITY_STATUS_NOT_SATISFIED.code to ResponseStatus.SECURITY_STATUS_NOT_SATISFIED,
        ResponseStatus.KEY_EXPIRED.code to ResponseStatus.KEY_EXPIRED,
        ResponseStatus.NO_KEY_REFERENCE.code to ResponseStatus.NO_KEY_REFERENCE,
        ResponseStatus.UNSUPPORTED_FUNCTION.code to ResponseStatus.UNSUPPORTED_FUNCTION,
        ResponseStatus.KEY_NOT_FOUND.code to ResponseStatus.KEY_NOT_FOUND
    )

    private val internalResponseMessages: Map<UShort, ResponseStatus> = mapOf(
        ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
        ResponseStatus.KEY_INVALID.code to ResponseStatus.KEY_INVALID,
        ResponseStatus.SECURITY_STATUS_NOT_SATISFIED.code to ResponseStatus.SECURITY_STATUS_NOT_SATISFIED,
        ResponseStatus.NO_KEY_REFERENCE.code to ResponseStatus.NO_KEY_REFERENCE,
        ResponseStatus.WRONG_TOKEN.code to ResponseStatus.WRONG_TOKEN,
        ResponseStatus.UNSUPPORTED_FUNCTION.code to ResponseStatus.UNSUPPORTED_FUNCTION,
        ResponseStatus.KEY_NOT_FOUND.code to ResponseStatus.KEY_NOT_FOUND
    )

    /**
     * Use-case 14.7.1 External Mutual Authentication command - gemSpec_COS#14.7.1
     * 
     * @param cmdData data from the external entity to verify on the target card
     * @param expectResponse whether to expect response data
     * @return the external mutual authentication command
     */
    @Throws(APDU.Error::class)
    fun externalMutualAuthentication(cmdData: ByteArray, expectResponse: Boolean = false): HealthCardCommand {
        return HealthCardCommandBuilder()
            .setCla(0x00u)
            .setIns(0x82u)
            .setP1(0x00u)
            .setP2(0x00u)
            .setData(cmdData)
            .setNe(if (expectResponse) APDU.EXPECTED_LENGTH_WILDCARD_SHORT else null)
            .setResponseStatuses(externalResponseMessages)
            .build()
    }

    /**
     * Use-case 14.7.4 Internal Authentication command - gemSpec_COS#14.7.4
     * 
     * @param token data to verify by the target card
     * @return the internal authentication command
     */
    @Throws(APDU.Error::class)
    fun internalAuthenticate(token: ByteArray): HealthCardCommand {
        return HealthCardCommandBuilder()
            .setCla(0x00u)
            .setIns(0x88u)
            .setP1(0x00u)
            .setP2(0x00u)
            .setData(token)
            .setNe(0)
            .setResponseStatuses(internalResponseMessages)
            .build()
    }
}

/**
 * PACE Protocol Commands
 * Use-case PACE #14.7.2.1 and #14.7.2.4
 */
object PaceCommands {

    private val responseMessages: Map<UShort, ResponseStatus> = mapOf(
        ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS
    )

    private fun builder(): HealthCardCommandBuilder {
        return HealthCardCommandBuilder()
            .setCla(0x10u)
            .setIns(0x86u)
            .setP1(0x00u)
            .setP2(0x00u)
            .setResponseStatuses(responseMessages)
    }

    /**
     * Start PACE General authenticate - step 1a #14.7.2.1.1
     * 
     * @return Step 1a command
     */
    fun step1a(): HealthCardCommand {
        return builder()
            .setData(byteArrayOf(0x7C, 0x00))
            .setNe(APDU.EXPECTED_LENGTH_WILDCARD_SHORT)
            .build()
    }

    /**
     * Send publicKey data (from response step 1b) command - step 2a #14.7.2.1.2
     * 
     * @param publicKey the COSb PCD PK1 to verify on COSa
     * @return Step 2a command
     */
    @Throws(Exception::class)
    fun step2a(publicKey: ByteArray): HealthCardCommand {
        val data = derEncoded(publicKey, 0x81)
        return builder()
            .setData(data)
            .setNe(APDU.EXPECTED_LENGTH_WILDCARD_SHORT)
            .build()
    }

    /**
     * Key agreement - step 3a #14.7.2.1.3
     * 
     * @param publicKey the COSb PCD PK2 to verify on COSa
     * @return Step 3a command
     */
    @Throws(Exception::class)
    fun step3a(publicKey: ByteArray): HealthCardCommand {
        val data = derEncoded(publicKey, 0x83)
        return builder()
            .setData(data)
            .setNe(APDU.EXPECTED_LENGTH_WILDCARD_SHORT)
            .build()
    }

    /**
     * Verify/Exchange token - step4a #14.7.2.1.4
     * 
     * @param token Exchange token. Must be 8 bytes long
     * @return Step 4a command
     */
    @Throws(HealthCardCommandBuilder.InvalidArgument::class)
    fun step4a(token: ByteArray): HealthCardCommand {
        if (token.size != 8) {
            throw HealthCardCommandBuilder.InvalidArgument.IllegalSize(token.size, 8)
        }

        val data = derEncoded(token, 0x85)
        return HealthCardCommandBuilder()
            .setCla(0x00u)
            .setIns(0x86u)
            .setP1(0x00u)
            .setP2(0x00u)
            .setData(data)
            .setNe(APDU.EXPECTED_LENGTH_WILDCARD_SHORT)
            .setResponseStatuses(responseMessages)
            .build()
    }

    /**
     * Encode data as DER wrapped in 0x7C application tag
     */
    private fun derEncoded(data: ByteArray, tag: Int): ByteArray {
        // Simple DER encoding: 0x7C [length] [tag] [length] [data]
        val innerLength = data.size
        val innerTlv = byteArrayOf(tag.toByte()) + encodeLength(innerLength) + data
        return byteArrayOf(0x7C.toByte()) + encodeLength(innerTlv.size) + innerTlv
    }

    private fun encodeLength(length: Int): ByteArray {
        return when {
            length < 0x80 -> byteArrayOf(length.toByte())
            length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
            else -> byteArrayOf(0x82.toByte(), (length shr 8).toByte(), (length and 0xFF).toByte())
        }
    }
}
