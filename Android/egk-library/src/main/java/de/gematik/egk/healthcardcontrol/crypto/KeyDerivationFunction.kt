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

package de.gematik.egk.healthcardcontrol.crypto

import java.security.MessageDigest

/**
 * Key derivation function for PACE protocol.
 * Derives symmetric keys from a shared secret using SHA-1 hash.
 */
object KeyDerivationFunction {
    
    /**
     * Key function type - only AES-128 is supported.
     */
    enum class KeyFuncType(val keySize: Int) {
        AES128(16)
    }
    
    /**
     * Key derivation mode.
     * Different counter values are used for different key purposes.
     */
    enum class Mode(val counterValue: Int) {
        /** Encryption key derivation */
        ENC(1),
        /** MAC key derivation */
        MAC(2),
        /** Password derivation (for CAN) */
        PASSWORD(3)
    }
    
    /**
     * Derive a key from input data using the specified mode.
     * Uses SHA-1 hash with a counter appended to the input.
     * 
     * @param input The input data (e.g., shared secret or CAN)
     * @param mode The derivation mode (ENC, MAC, or PASSWORD)
     * @param keyFunc The key function type (default: AES128)
     * @return The derived key
     */
    @JvmStatic
    @JvmOverloads
    fun deriveKey(
        input: ByteArray,
        mode: Mode,
        keyFunc: KeyFuncType = KeyFuncType.AES128
    ): ByteArray {
        // Append 4-byte counter to input
        val counter = byteArrayOf(
            0x00,
            0x00,
            0x00,
            mode.counterValue.toByte()
        )
        val dataToHash = input + counter
        
        // Calculate SHA-1 hash
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(dataToHash)
        
        // Return first keySize bytes
        return hash.copyOf(keyFunc.keySize)
    }
    
    /**
     * Derive a key from a string input (e.g., CAN).
     * 
     * @param input The input string (will be converted to ASCII bytes)
     * @param mode The derivation mode
     * @param keyFunc The key function type (default: AES128)
     * @return The derived key
     */
    @JvmStatic
    @JvmOverloads
    fun deriveKey(
        input: String,
        mode: Mode,
        keyFunc: KeyFuncType = KeyFuncType.AES128
    ): ByteArray {
        return deriveKey(input.toByteArray(Charsets.US_ASCII), mode, keyFunc)
    }
}
