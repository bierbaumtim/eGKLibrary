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
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * File Identifier - gemSpec_COS 8.1.1 #N006.700, N006.900
 */
class FileIdentifier private constructor(
    override val rawValue: ByteArray
) : CardObjectIdentifierType, CardItemType {

    sealed class Error : Exception() {
        data class IllegalArgument(val argument: String) : Error() {
            override val message: String = "File Identifier is invalid. [$argument]"
        }
        data class InvalidLength(val length: Int) : Error() {
            override val message: String = "File Identifier has invalid length: $length (expected 2)"
        }
    }

    companion object {
        /**
         * Sanity check for file identifier
         *
         * @see gemSpec_COS 8.1.1 (#N006.700, N006.900)
         *
         * @param value the byte array that should make up the FID
         * @return Result success with the data when the value could represent a FID
         */
        fun isValid(value: ByteArray): Result<ByteArray> {
            if (value.size != 2) {
                return Result.failure(Error.InvalidLength(value.size))
            }

            val fid = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF

            val isValidRange = (fid in 0x1000..0xFEFF && fid != 0x3FFF) || fid == 0x011C
            return if (isValidRange) {
                Result.success(value)
            } else {
                Result.failure(Error.IllegalArgument("File Identifier invalid: [0x${value.toHexString()}]"))
            }
        }

        /**
         * Create a FileIdentifier from ByteArray
         *
         * @param data the raw FID data
         * @throws Error if the data is invalid
         */
        @Throws(Error::class)
        fun create(data: ByteArray): FileIdentifier {
            return FileIdentifier(isValid(data).getOrThrow())
        }

        /**
         * Create a FileIdentifier from hex string
         *
         * @param hex the hex string representation of the FID
         * @throws Error if the hex string is invalid
         */
        @Throws(Error::class)
        fun create(hex: String): FileIdentifier {
            val data = try {
                hex.hexToByteArray()
            } catch (e: IllegalArgumentException) {
                throw Error.IllegalArgument("File Identifier is invalid (non-hex characters found). [$hex]")
            }
            return create(data)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileIdentifier) return false
        return rawValue.contentEquals(other.rawValue)
    }

    override fun hashCode(): Int = rawValue.contentHashCode()

    override fun toString(): String = "[0x${rawValue.toHexString()}]"
}

/**
 * Create a FileIdentifier from a hex string literal
 */
fun String.toFileIdentifier(): FileIdentifier = FileIdentifier.create(this)
