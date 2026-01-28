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

package de.gematik.egk.healthcardaccess.util

/**
 * Extension to convert ByteArray to hex string
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

/**
 * Extension to convert ByteArray to lowercase hex string
 */
fun ByteArray.toHexStringLower(): String = joinToString("") { "%02x".format(it) }

/**
 * Extension to convert hex string to ByteArray
 * 
 * @throws IllegalArgumentException if the string contains non-hex characters or has odd length
 */
fun String.hexToByteArray(): ByteArray {
    val cleanHex = this.replace(" ", "")
    require(cleanHex.length % 2 == 0) { "Hex string must have even length: $this" }
    
    return ByteArray(cleanHex.length / 2) { index ->
        val i = index * 2
        val hex = cleanHex.substring(i, i + 2)
        hex.toIntOrNull(16)?.toByte()
            ?: throw IllegalArgumentException("Invalid hex characters at position $i: $hex")
    }
}

/**
 * Try to convert hex string to ByteArray, returns null on failure
 */
fun String.hexToByteArrayOrNull(): ByteArray? {
    return try {
        hexToByteArray()
    } catch (e: IllegalArgumentException) {
        null
    }
}
