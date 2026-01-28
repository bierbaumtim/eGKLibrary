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

/**
 * Errors that can occur during NFC card operations.
 */
sealed class NFCCardError : Exception() {
    /** No card is currently present */
    object NoCardPresent : NFCCardError() {
        override val message: String = "No NFC card present"
    }
    
    /** Card communication timeout */
    object SendTimeout : NFCCardError() {
        override val message: String = "NFC communication timeout"
    }
    
    /** Transfer/communication exception */
    data class TransferException(val errorMessage: String) : NFCCardError() {
        override val message: String = errorMessage
    }
    
    /** Tag was lost during communication */
    object TagLost : NFCCardError() {
        override val message: String = "NFC tag was lost"
    }
    
    /** Tag is not connected */
    object NotConnected : NFCCardError() {
        override val message: String = "NFC tag is not connected"
    }
    
    /** Unsupported NFC tag type */
    object UnsupportedTag : NFCCardError() {
        override val message: String = "Unsupported NFC tag type"
    }
}
