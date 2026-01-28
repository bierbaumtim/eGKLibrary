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
import de.gematik.egk.cardreaderproviderapi.card.CardType
import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.cardreaderproviderapi.command.ResponseType
import de.gematik.egk.healthcardaccess.HealthCardType
import de.gematik.egk.healthcardaccess.util.toHexString

/**
 * A secure card channel that encrypts/decrypts APDU commands/responses using SecureMessaging.
 */
class SecureCardChannel(
    private val session: SecureMessaging,
    private val healthCard: HealthCardType
) : CardChannelType {
    
    private val channel: CardChannelType = healthCard.currentCardChannel
    
    override val card: CardType
        get() = channel.card
    
    override val channelNumber: Int
        get() = channel.channelNumber
    
    override val extendedLengthSupported: Boolean
        get() = channel.extendedLengthSupported
    
    override val maxMessageLength: Int
        get() = channel.maxMessageLength
    
    override val maxResponseLength: Int
        get() = channel.maxResponseLength
    
    override suspend fun transmit(command: CommandType, writeTimeout: Long, readTimeout: Long): ResponseType {
        // Log the command header only (to prevent logging PIN)
        val headerHex = byteArrayOf(command.cla.toByte(), command.ins.toByte(), command.p1.toByte(), command.p2.toByte()).toHexString()
        android.util.Log.d(TAG, ">> $headerHex...")
        
        val encryptedCommand = session.encrypt(command)
        val encryptedResponse = channel.transmit(encryptedCommand, writeTimeout, readTimeout)
        val decryptedApdu = session.decrypt(encryptedResponse)
        
        // Log the response
        val sw = (decryptedApdu.sw1.toInt() and 0xFF shl 8) or (decryptedApdu.sw2.toInt() and 0xFF)
        android.util.Log.d(TAG, "<< ${String.format("%04X", sw)} | [${decryptedApdu.data?.toHexString() ?: ""}]")
        
        return decryptedApdu
    }
    
    override suspend fun close() {
        session.invalidate()
        channel.close()
    }
    
    companion object {
        private const val TAG = "SecureCardChannel"
    }
}
