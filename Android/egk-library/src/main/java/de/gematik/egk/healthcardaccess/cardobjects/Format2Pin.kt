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

/**
 * PIN block, formatted according to gemSpec_COS 8.1.7
 * and [ISO9564](https://en.wikipedia.org/wiki/ISO_9564) standard
 */
class Format2Pin private constructor(
    /**
     * PIN block
     * ****************************************
     * PAN:            43219876543210987
     * PIN:            1234
     * PAD:            N/A
     * Format:         Format 2 (ISO-2)
     * ----------------------------------------
     * Clear PIN block:241234FFFFFFFFFF
     */
    val pin: ByteArray
) : CardItemType {

    sealed class Error : Exception() {
        data class IllegalArgument(val argument: String) : Error() {
            override val message: String = "Invalid pin. [$argument]"
        }
    }

    companion object {
        private const val MIN_PIN_LEN = 4 // gemSpec_COS#N008.000
        private const val MAX_PIN_LEN = 12 // gemSpec_COS#N008.000
        
        private val PIN_REGEX = Regex("^[0-9]{$MIN_PIN_LEN,$MAX_PIN_LEN}$")

        /**
         * Format and check pincode
         *
         * @param pincode min 4 and max 12 character string consisting of numbers only
         * @throws Error.IllegalArgument when the pincode param is invalid
         */
        @Throws(Error::class)
        fun create(pincode: String): Format2Pin {
            // gemSpec_COS#N008.000
            if (!PIN_REGEX.matches(pincode)) {
                throw Error.IllegalArgument("Invalid pin: [$pincode] does not conform to regex: [${PIN_REGEX.pattern}]")
            }

            // gemSpec_COS#N008.100.b,c,d,e
            val paddedPin = pincode.padEnd(14, 'F')
            val buffer = String.format("2%X%s", pincode.length, paddedPin)

            // gemSpec_COS#N008.100.a
            val pin = buffer.hexToByteArray()
            return Format2Pin(pin)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Format2Pin) return false
        return pin.contentEquals(other.pin)
    }

    override fun hashCode(): Int = pin.contentHashCode()

    override fun toString(): String = "Format2Pin[****]"
}

/**
 * Create a Format2Pin from a pincode string
 */
fun String.toFormat2Pin(): Format2Pin = Format2Pin.create(this)
