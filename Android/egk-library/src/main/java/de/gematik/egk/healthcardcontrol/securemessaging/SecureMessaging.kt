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

import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.cardreaderproviderapi.command.ResponseType

/**
 * Component that attached to a SecureCardChannel takes care of message de-/encryption.
 */
interface SecureMessaging {
    
    /**
     * Encrypt the APDU Command.
     * 
     * @param command The APDU command to encrypt
     * @return The APDU command encrypted accordingly to the SecureMessaging protocol
     * @throws SecureMessagingException if encryption fails
     */
    fun encrypt(command: CommandType): CommandType
    
    /**
     * Decrypt the APDU Response.
     * 
     * @param response The APDU response to decrypt
     * @return The APDU response decrypted accordingly to the SecureMessaging protocol
     * @throws SecureMessagingException if decryption fails
     */
    fun decrypt(response: ResponseType): ResponseType
    
    /**
     * Destruct the information held by this object.
     * Securely clears keys and other sensitive data.
     */
    fun invalidate()
}

/**
 * Exceptions related to SecureMessaging operations.
 */
sealed class SecureMessagingException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** APDU is already encrypted */
    class ApduAlreadyEncrypted : SecureMessagingException("APDU is already encrypted")
    
    /** Encrypted response is malformed */
    class EncryptedResponseMalformed : SecureMessagingException("Encrypted response is malformed")
    
    /** MAC verification failed */
    class MacVerificationFailed : SecureMessagingException("Secure messaging MAC verification failed")
}
