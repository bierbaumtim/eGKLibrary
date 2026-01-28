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
import de.gematik.egk.cardreaderproviderapi.card.CardError
import de.gematik.egk.cardreaderproviderapi.card.CardType
import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.healthcardaccess.commands.HealthCardCommand
import de.gematik.egk.healthcardaccess.model.HealthCardStatus
import de.gematik.egk.healthcardaccess.responses.HealthCardResponse

/**
 * HealthCard is the class that is used to bind the CardReaderProviderAPI channel to the HealthCardControl layer
 */
class HealthCard(
    private val card: CardType,
    initialStatus: HealthCardStatus = HealthCardStatus.Unknown,
    channel: CardChannelType? = null
) : HealthCardType {

    private val channels = mutableListOf<CardChannelType>()

    /** The current card channel to use to send/receive APDUs over */
    override var currentCardChannel: CardChannelType
        private set

    /** The status of the card and channel */
    override var status: HealthCardStatus = initialStatus
        private set

    init {
        currentCardChannel = channel ?: try {
            card.openBasicChannel().also { channels.add(it) }
        } catch (e: CardError) {
            throw e
        }
    }

    /**
     * Initialize a HealthCard with a card channel and set its initial status
     *
     * @param card The associated Card
     * @param status Initial status (default: .unknown)
     * @throws CardError when opening the basic channel throws
     */
    companion object {
        /**
         * Create a HealthCard from a CardType
         *
         * @param card The card to create a HealthCard from
         * @param status The initial status (default: Unknown)
         * @return The created HealthCard
         * @throws CardError when opening the basic channel fails
         */
        @Throws(CardError::class)
        suspend fun create(card: CardType, status: HealthCardStatus = HealthCardStatus.Unknown): HealthCard {
            return HealthCard(card, status)
        }
    }

    /**
     * Update the health card status
     */
    fun updateStatus(newStatus: HealthCardStatus) {
        status = newStatus
    }
    
    /**
     * Transmit a HealthCardCommand to the card.
     */
    override suspend fun transmit(command: HealthCardCommand): HealthCardResponse {
        val response = currentCardChannel.transmit(command.apduCommand)
        return HealthCardResponse.fromResponse(response)
    }
    
    /**
     * Transmit a raw APDU command to the card.
     */
    override suspend fun transmit(command: CommandType): HealthCardResponse {
        val response = currentCardChannel.transmit(command)
        return HealthCardResponse.fromResponse(response)
    }
}
