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

package de.gematik.egk.nfccardreaderprovider

import android.nfc.tech.IsoDep
import de.gematik.egk.cardreaderproviderapi.card.CardChannelType
import de.gematik.egk.cardreaderproviderapi.card.CardError
import de.gematik.egk.cardreaderproviderapi.card.CardType
import de.gematik.egk.cardreaderproviderapi.command.APDU
import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.cardreaderproviderapi.command.ResponseType
import de.gematik.egk.healthcardaccess.util.toHexString
import android.util.Log

/**
 * NFC Card Channel implementation using Android's IsoDep API.
 */
class NFCCardChannel(
    private val nfcCard: NFCCard,
    private val isoDep: IsoDep,
    override val channelNumber: Int = 0
) : CardChannelType {
    
    companion object {
        private const val TAG = "NFCCardChannel"
    }
    
    override val card: CardType
        get() = nfcCard
    
    override val extendedLengthSupported: Boolean = true
    
    override val maxMessageLength: Int
        get() = isoDep.maxTransceiveLength
    
    override val maxResponseLength: Int
        get() = 0x10000
    
    override suspend fun transmit(command: CommandType, writeTimeout: Long, readTimeout: Long): ResponseType {
        val commandApdu: CommandType = if (channelNumber > 0) {
            command.toLogicalChannel(channelNumber.toUByte())
        } else {
            command
        }
        
        if (!isoDep.isConnected) {
            throw CardError.ConnectionError(NFCCardError.NotConnected)
        }
        
        val commandBytes = commandApdu.bytes
        Log.d(TAG, "SEND:     [${commandBytes.toHexString()}]")
        
        // Set timeout if specified
        if (readTimeout > 0) {
            isoDep.timeout = readTimeout.toInt()
        }
        
        return try {
            val responseBytes = isoDep.transceive(commandBytes)
            Log.d(TAG, "RESPONSE: [${responseBytes.toHexString()}]")
            
            if (responseBytes.size < 2) {
                throw CardError.ConnectionError(
                    NFCCardError.TransferException("Response too short: ${responseBytes.size} bytes")
                )
            }
            
            val sw1 = responseBytes[responseBytes.size - 2].toUByte()
            val sw2 = responseBytes[responseBytes.size - 1].toUByte()
            val data = if (responseBytes.size > 2) {
                responseBytes.copyOfRange(0, responseBytes.size - 2)
            } else {
                byteArrayOf()
            }
            
            APDU.Response.create(data, sw1, sw2)
        } catch (e: android.nfc.TagLostException) {
            throw CardError.ConnectionError(NFCCardError.TagLost)
        } catch (e: java.io.IOException) {
            throw CardError.ConnectionError(NFCCardError.TransferException(e.message ?: "IO error"))
        }
    }
    
    override suspend fun close() {
        // Channel close is handled by NFCCard
    }
}

/**
 * Extension to convert command to a logical channel variant.
 */
private fun CommandType.toLogicalChannel(channelNo: UByte): CommandType {
    // Modify CLA byte to indicate logical channel
    val newCla = ((this.cla.toInt() and 0xFC) or (channelNo.toInt() and 0x03)).toUByte()
    return APDU.Command.create(newCla, this.ins, this.p1, this.p2, this.data, this.ne)
}
