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

package de.gematik.egk.healthcardaccess.cardobjects

/**
 * Key identifier for cryptographic operations
 * @see gemSpec_COS#N016.400 and #N017.100
 */
class Key private constructor(
    val keyId: UByte
) : CardItemType, CardKeyReferenceType {

    sealed class Error : Exception() {
        data class IllegalArgument(val argument: String) : Error() {
            override val message: String = "Key ID is invalid. [$argument]"
        }
    }

    companion object {
        private const val MIN_KEY_ID = 2
        private const val MAX_KEY_ID = 28

        /**
         * Create a Key from UByte value
         *
         * @param key the key ID (2-28)
         * @throws Error.IllegalArgument if the value is out of range
         */
        @Throws(Error::class)
        fun create(key: UByte): Key {
            if (key.toInt() < MIN_KEY_ID || key.toInt() > MAX_KEY_ID) {
                // gemSpec_COS#N016.400 and #N017.100
                throw Error.IllegalArgument("Key ID: [$key] out of range [$MIN_KEY_ID,$MAX_KEY_ID]")
            }
            return Key(key)
        }
    }

    override fun calculateKeyReference(dfSpecific: Boolean): UByte {
        // gemSpec_COS#N099.600
        return if (dfSpecific) {
            (keyId + DF_SPECIFIC_PWD_MARKER).toUByte()
        } else {
            keyId
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Key) return false
        return keyId == other.keyId
    }

    override fun hashCode(): Int = keyId.hashCode()

    override fun toString(): String = "Key[$keyId]"
}
