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
 * Short File Identifier - gemSpec_COS#N007.000
 */
class ShortFileIdentifier private constructor(
    override val rawValue: ByteArray
) : CardObjectIdentifierType, CardItemType {

    sealed class Error : Exception() {
        data class IllegalArgument(val argument: String) : Error() {
            override val message: String = "Short File Identifier is invalid. [$argument]"
        }
    }

    companion object {
        private const val SFID_MIN = 1
        private const val SFID_MAX = 30 // inclusive

        /**
         * Sanity check for short file identifier
         */
        fun isValid(value: ByteArray): Result<ByteArray> {
            if (value.size != 1) {
                return Result.failure(Error.IllegalArgument("Short File Identifier is invalid: [0x${value.toHexString()}]"))
            }
            return isValid(value[0].toUByte())
        }

        /**
         * Sanity check for short file identifier value
         */
        fun isValid(value: UByte): Result<ByteArray> {
            return if (value.toInt() in SFID_MIN..SFID_MAX) {
                Result.success(byteArrayOf(value.toByte()))
            } else {
                Result.failure(Error.IllegalArgument("Short File Identifier is invalid: [0x${String.format("%02X", value.toByte())}]"))
            }
        }

        /**
         * Init the Short File Identifier from ASN.1 formatted primitive
         */
        @Throws(Error::class)
        fun createFromAsn1(data: ByteArray): ShortFileIdentifier {
            if (data.size != 1) {
                throw Error.IllegalArgument("Parsing [ASN.1] Short File Identifier is invalid: [0x${data.toHexString()}]")
            }
            return create(((data[0].toInt() and 0xFF) shr 3).toUByte())
        }

        /**
         * Create a ShortFileIdentifier from hex string
         */
        @Throws(Error::class)
        fun create(hex: String): ShortFileIdentifier {
            val data = try {
                hex.hexToByteArray()
            } catch (e: IllegalArgumentException) {
                throw Error.IllegalArgument("Short File Identifier is invalid (non-hex characters found). [$hex]")
            }
            if (data.size != 1) {
                throw Error.IllegalArgument("Short File Identifier is invalid. [$hex]")
            }
            return create(data[0].toUByte())
        }

        /**
         * Create a ShortFileIdentifier from UByte value
         */
        @Throws(Error::class)
        fun create(value: UByte): ShortFileIdentifier {
            return ShortFileIdentifier(isValid(value).getOrThrow())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShortFileIdentifier) return false
        return rawValue.contentEquals(other.rawValue)
    }

    override fun hashCode(): Int = rawValue.contentHashCode()

    override fun toString(): String = "[0x${rawValue.toHexString()}]"
}

/**
 * Create a ShortFileIdentifier from a hex string literal
 */
fun String.toShortFileIdentifier(): ShortFileIdentifier = ShortFileIdentifier.create(this)
