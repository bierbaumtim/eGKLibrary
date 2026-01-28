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

import de.gematik.egk.cardreaderproviderapi.card.CardChannelType
import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.healthcardaccess.HealthCardType
import de.gematik.egk.healthcardaccess.commands.HealthCardCommand
import de.gematik.egk.healthcardaccess.model.HealthCardStatus
import de.gematik.egk.healthcardaccess.responses.HealthCardResponse

/**
 * A HealthCard with secure messaging (PACE) established.
 * All commands sent through this card will be encrypted and
 * responses will be decrypted automatically.
 */
class SecureHealthCard(
    private val baseCard: HealthCardType,
    private val secureMessaging: SecureMessaging
) : HealthCardType {
    
    private val secureChannel = SecureCardChannel(secureMessaging, baseCard)
    
    override val currentCardChannel: CardChannelType
        get() = secureChannel
    
    override val status: HealthCardStatus
        get() = baseCard.status
    
    override suspend fun transmit(command: HealthCardCommand): HealthCardResponse {
        val response = secureChannel.transmit(command.apduCommand)
        return HealthCardResponse.fromResponse(response)
    }
    
    override suspend fun transmit(command: CommandType): HealthCardResponse {
        val response = secureChannel.transmit(command)
        return HealthCardResponse.fromResponse(response)
    }
    
    /**
     * Invalidate the secure messaging session.
     * After calling this, the card can no longer be used for secure operations.
     */
    fun invalidate() {
        secureMessaging.invalidate()
    }
}

/**
 * Extension function to open a secure session on a health card.
 * 
 * @param can The Card Access Number
 * @return A SecureHealthCard with PACE secure messaging established
 */
suspend fun HealthCardType.openSecureSession(can: String): SecureHealthCard {
    val secureMessaging = KeyAgreement.negotiateSessionKey(this, can)
    return SecureHealthCard(this, secureMessaging)
}
