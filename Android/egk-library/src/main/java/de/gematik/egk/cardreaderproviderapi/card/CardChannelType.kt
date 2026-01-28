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

package de.gematik.egk.cardreaderproviderapi.card

import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.cardreaderproviderapi.command.ResponseType

/**
 * General card communications protocol
 */
interface CardChannelType {
    /**
     * The associated card with this channel
     * Note: implementers may choose to hold a weak reference
     */
    val card: CardType

    /**
     * The channel number
     */
    val channelNumber: Int

    /**
     * Identify whether a channel supports APDU extended length commands/responses
     */
    val extendedLengthSupported: Boolean

    /**
     * Max length of APDU body in bytes.
     * Note: HealthCards COS typically support up to max 4096 byte body-length
     */
    val maxMessageLength: Int

    /**
     * Max length of an APDU response.
     */
    val maxResponseLength: Int

    /**
     * Transceive a (APDU) command
     *
     * @param command the prepared command
     * @param writeTimeout the max waiting time in milliseconds before the first byte should have been sent.
     *                     (<= 0 = no timeout)
     * @param readTimeout the max waiting time in milliseconds before the first byte should have been received.
     *                    (<= 0 = no timeout)
     *
     * @throws CardError when transmitting and/or receiving the response failed
     *
     * @return the Command APDU Response or CardError on failure
     */
    @Throws(CardError::class)
    suspend fun transmit(
        command: CommandType,
        writeTimeout: Long = 0,
        readTimeout: Long = 0
    ): ResponseType

    /**
     * Close the channel for subsequent actions.
     *
     * @throws CardError
     */
    @Throws(CardError::class)
    suspend fun close()
}

/**
 * Extension function to transmit with default timeouts
 */
suspend fun CardChannelType.transmit(command: CommandType): ResponseType {
    return transmit(command, 0, 0)
}
