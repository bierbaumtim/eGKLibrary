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

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES cryptographic operations for PACE protocol.
 */
object AES {
    
    init {
        // Ensure BouncyCastle provider is registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
    
    /**
     * AES-128-CBC encryption and decryption operations.
     */
    object CBC128 {
        private const val BLOCK_SIZE = 16
        private val ZERO_IV = ByteArray(BLOCK_SIZE)
        
        /**
         * Encrypt data using AES-128-CBC.
         * 
         * @param data The data to encrypt
         * @param key The 16-byte encryption key
         * @param initVector The initialization vector (defaults to zero IV)
         * @return The encrypted data
         * @throws IllegalArgumentException if key is not 16 bytes
         */
        @JvmStatic
        @JvmOverloads
        fun encrypt(
            data: ByteArray,
            key: ByteArray,
            initVector: ByteArray = ZERO_IV
        ): ByteArray {
            require(key.size == BLOCK_SIZE) { "Key must be 16 bytes for AES-128" }
            require(initVector.size == BLOCK_SIZE) { "IV must be 16 bytes" }
            
            val cipher = Cipher.getInstance("AES/CBC/NoPadding", BouncyCastleProvider.PROVIDER_NAME)
            val secretKey = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(initVector)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            return cipher.doFinal(data)
        }
        
        /**
         * Decrypt data using AES-128-CBC.
         * 
         * @param data The data to decrypt
         * @param key The 16-byte decryption key
         * @param initVector The initialization vector (defaults to zero IV)
         * @return The decrypted data
         * @throws IllegalArgumentException if key is not 16 bytes
         */
        @JvmStatic
        @JvmOverloads
        fun decrypt(
            data: ByteArray,
            key: ByteArray,
            initVector: ByteArray = ZERO_IV
        ): ByteArray {
            require(key.size == BLOCK_SIZE) { "Key must be 16 bytes for AES-128" }
            require(initVector.size == BLOCK_SIZE) { "IV must be 16 bytes" }
            
            val cipher = Cipher.getInstance("AES/CBC/NoPadding", BouncyCastleProvider.PROVIDER_NAME)
            val secretKey = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(initVector)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            return cipher.doFinal(data)
        }
    }
    
    /**
     * Calculate AES-CMAC (Cipher-based Message Authentication Code).
     * 
     * @param key The 16-byte MAC key
     * @param data The data to authenticate
     * @return The 16-byte CMAC
     * @throws IllegalArgumentException if key is not 16 bytes
     */
    @JvmStatic
    fun cmac(key: ByteArray, data: ByteArray): ByteArray {
        require(key.size == 16) { "Key must be 16 bytes for AES-128 CMAC" }
        
        val mac = CMac(AESEngine.newInstance(), 128)
        mac.init(KeyParameter(key))
        mac.update(data, 0, data.size)
        val result = ByteArray(mac.macSize)
        mac.doFinal(result, 0)
        return result
    }
}
