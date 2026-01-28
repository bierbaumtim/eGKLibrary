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
 * Password Identifier as per gemSpec_COS#N015.000
 */
class Password private constructor(
    val pwdId: UByte
) : CardItemType, CardKeyReferenceType {

    sealed class Error : Exception() {
        data class IllegalArgument(val argument: String) : Error() {
            override val message: String = "Password value is invalid. [$argument]"
        }
    }

    companion object {
        private const val MIN_PWD_ID: UByte = 0u // gemSpec_COS#N015.000
        private const val MAX_PWD_ID: UByte = 31u // gemSpec_COS#N015.000

        /**
         * Create a Password from UByte value
         *
         * @param value the password ID (0-31)
         * @throws Error.IllegalArgument if the value is out of range
         */
        @Throws(Error::class)
        fun create(value: UByte): Password {
            if (value > MAX_PWD_ID) {
                throw Error.IllegalArgument("Password value is invalid: [$value]")
            }
            return Password(value)
        }

        /**
         * Create a Password from hex string
         *
         * @param hex the hex string representation of the password ID
         * @throws Error.IllegalArgument if the hex string is invalid
         */
        @Throws(Error::class)
        fun create(hex: String): Password {
            require(hex.isNotEmpty() && hex.length <= 2) {
                throw Error.IllegalArgument("[String Literal] Password value is invalid: [$hex]")
            }
            val value = hex.toIntOrNull(16)?.toUByte()
                ?: throw Error.IllegalArgument("[String Literal] Password value is invalid: [$hex]")
            return create(value)
        }
    }

    override fun calculateKeyReference(dfSpecific: Boolean): UByte {
        // gemSpec_COS#N072.800
        return if (dfSpecific) {
            (pwdId + DF_SPECIFIC_PWD_MARKER).toUByte()
        } else {
            pwdId
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Password) return false
        return pwdId == other.pwdId
    }

    override fun hashCode(): Int = pwdId.hashCode()

    override fun toString(): String = "Password[$pwdId]"
}

/**
 * Create a Password from a hex string literal
 */
fun String.toPassword(): Password = Password.create(this)
