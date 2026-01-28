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

import de.gematik.egk.healthcardaccess.util.hexToByteArray
import de.gematik.egk.healthcardaccess.util.toHexString

/**
 * ApplicationIdentifier representation to prevent (accidental) misuse
 * E.g. using any 'random' String as function parameter where an AID is expected
 * 
 * @see gemSpec_COS#N010.200
 */
class ApplicationIdentifier private constructor(
    override val rawValue: ByteArray
) : CardObjectIdentifierType, CardItemType {

    sealed class Error : Exception() {
        data class IllegalArgument(val argument: String) : Error() {
            override val message: String = "Application File Identifier is invalid. [$argument]"
        }
        data class InvalidLength(val length: Int) : Error() {
            override val message: String = "Application Identifier has invalid length: $length (expected 5-16)"
        }
    }

    companion object {
        private const val AID_MIN_LENGTH = 5
        private const val AID_MAX_LENGTH = 16

        /**
         * Sanity check for application file identifier
         *
         * @param value the byte array that should make up the AID
         * @return Result success with the data when the value could represent an AID
         */
        fun isValid(value: ByteArray): Result<ByteArray> {
            return if (value.size < AID_MIN_LENGTH || value.size > AID_MAX_LENGTH) {
                Result.failure(Error.InvalidLength(value.size))
            } else {
                Result.success(value)
            }
        }

        /**
         * Create an ApplicationIdentifier from ByteArray
         * 
         * @param data the raw AID data
         * @throws Error.InvalidLength if the length is not in the valid range
         */
        @Throws(Error::class)
        fun create(data: ByteArray): ApplicationIdentifier {
            return ApplicationIdentifier(isValid(data).getOrThrow())
        }

        /**
         * Create an ApplicationIdentifier from hex string
         * 
         * @param hex the hex string representation of the AID
         * @throws Error.IllegalArgument if the hex string is invalid
         * @throws Error.InvalidLength if the length is not in the valid range
         */
        @Throws(Error::class)
        fun create(hex: String): ApplicationIdentifier {
            val data = try {
                hex.hexToByteArray()
            } catch (e: IllegalArgumentException) {
                throw Error.IllegalArgument("Application File Identifier is invalid (non-hex characters found). [$hex]")
            }
            return create(data)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApplicationIdentifier) return false
        return rawValue.contentEquals(other.rawValue)
    }

    override fun hashCode(): Int = rawValue.contentHashCode()

    override fun toString(): String = "[0x${rawValue.toHexString()}]"
}

/**
 * Create an ApplicationIdentifier from a hex string literal
 */
fun String.toApplicationIdentifier(): ApplicationIdentifier = ApplicationIdentifier.create(this)
