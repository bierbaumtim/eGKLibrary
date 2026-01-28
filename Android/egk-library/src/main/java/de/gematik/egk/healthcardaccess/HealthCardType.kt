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

package de.gematik.egk.healthcardaccess

import de.gematik.egk.cardreaderproviderapi.card.CardChannelType
import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.healthcardaccess.commands.HealthCardCommand
import de.gematik.egk.healthcardaccess.model.HealthCardStatus
import de.gematik.egk.healthcardaccess.responses.HealthCardResponse

/**
 * Protocol/Interface that defines the capabilities of a health card
 */
interface HealthCardType {
    /** The current card channel to use to send/receive APDUs over */
    val currentCardChannel: CardChannelType

    /** The status of the card and channel */
    val status: HealthCardStatus

    /** The number of the current card channel */
    val channelNumber: Int
        get() = currentCardChannel.channelNumber

    /** Identify whether the current card channel supports APDU extended length commands/responses */
    val extendedLengthSupported: Boolean
        get() = currentCardChannel.extendedLengthSupported

    /** Max length of a APDU body in bytes supported by the current card channel */
    val maxMessageLength: Int
        get() = currentCardChannel.maxMessageLength

    /** Max length of a APDU response supported by the current card channel */
    val maxResponseLength: Int
        get() = currentCardChannel.maxResponseLength
    
    /**
     * Transmit a HealthCard command to the card.
     * 
     * @param command The HealthCardCommand to transmit
     * @return HealthCardResponse containing the response data and status
     */
    suspend fun transmit(command: HealthCardCommand): HealthCardResponse
    
    /**
     * Transmit a raw APDU command to the card.
     * 
     * @param command The CommandType to transmit
     * @return HealthCardResponse containing the response data and status
     */
    suspend fun transmit(command: CommandType): HealthCardResponse
}
