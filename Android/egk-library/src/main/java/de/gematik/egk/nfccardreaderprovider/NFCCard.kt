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

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import de.gematik.egk.cardreaderproviderapi.card.CardChannelType
import de.gematik.egk.cardreaderproviderapi.card.CardError
import de.gematik.egk.cardreaderproviderapi.card.CardProtocol
import de.gematik.egk.cardreaderproviderapi.card.CardType
import de.gematik.egk.cardreaderproviderapi.command.APDU

/**
 * NFC Card implementation using Android's IsoDep API.
 */
class NFCCard(
    private val tag: Tag,
    private val isoDep: IsoDep
) : CardType {
    
    companion object {
        private const val TAG = "NFCCard"
        
        /**
         * Create an NFCCard from an NFC Tag if it supports IsoDep.
         * 
         * @param tag The NFC tag
         * @return NFCCard instance or null if IsoDep is not supported
         */
        fun fromTag(tag: Tag): NFCCard? {
            val isoDep = IsoDep.get(tag) ?: return null
            return NFCCard(tag, isoDep)
        }
    }
    
    private var basicChannel: NFCCardChannel? = null
    
    override val atr: ByteArray
        get() = isoDep.historicalBytes ?: ByteArray(0)
    
    override val protocol: Set<CardProtocol> = setOf(CardProtocol.T1)
    
    /**
     * Connect to the NFC tag.
     * Must be called before any operations.
     */
    fun connect() {
        if (!isoDep.isConnected) {
            isoDep.connect()
            Log.d(TAG, "Connected to NFC tag. Max transceive length: ${isoDep.maxTransceiveLength}")
        }
    }
    
    /**
     * Check if the tag is connected.
     */
    val isConnected: Boolean
        get() = isoDep.isConnected
    
    override fun openBasicChannel(): CardChannelType {
        basicChannel?.let { return it }
        
        if (!isoDep.isConnected) {
            throw CardError.ConnectionError(NFCCardError.NotConnected)
        }
        
        val channel = NFCCardChannel(this, isoDep)
        basicChannel = channel
        return channel
    }
    
    override suspend fun openLogicChannel(): CardChannelType {
        if (!isoDep.isConnected) {
            throw CardError.ConnectionError(NFCCardError.NotConnected)
        }
        
        // Send MANAGE CHANNEL command to open a new logical channel
        val manageChannelCommandOpen = APDU.Command(
            cla = 0x00u,
            ins = 0x70u,
            p1 = 0x00u,
            p2 = 0x00u,
            ne = 0x01
        )
        
        val response = openBasicChannel().transmit(manageChannelCommandOpen)
        
        if (response.sw != 0x9000) {
            throw CardError.ConnectionError(
                NFCCardError.TransferException(
                    "openLogicalChannel failed, response code: ${String.format("0x%04X", response.sw)}"
                )
            )
        }
        
        val responseData = response.data
        if (responseData == null || responseData.isEmpty()) {
            throw CardError.ConnectionError(
                NFCCardError.TransferException("openLogicalChannel failed, no channel number received")
            )
        }
        
        return NFCCardChannel(this, isoDep, responseData[0].toInt())
    }
    
    override fun initialApplicationIdentifier(): ByteArray? {
        // Android's IsoDep doesn't provide the initially selected AID directly
        // Return null or implement based on specific requirements
        return null
    }
    
    override fun disconnect(reset: Boolean) {
        Log.d(TAG, "Disconnecting NFC card...")
        basicChannel = null
        if (isoDep.isConnected) {
            try {
                isoDep.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing IsoDep: ${e.message}")
            }
        }
        Log.d(TAG, "NFC card disconnected")
    }
    
    override val description: String = "NFCCard"
}
