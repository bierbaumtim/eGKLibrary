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

package de.gematik.egk.cardreaderproviderapi.command

import java.io.ByteArrayOutputStream

/**
 * Concrete APDU command + response implementation
 */
object APDU {

    sealed class Error : Exception() {
        /** when the APDU body exceeds 65535 */
        object CommandBodyDataTooLarge : Error()
        
        /** when the expected APDU response length is out of bounds [0, 65536] */
        object ExpectedResponseLengthOutOfBounds : Error()
        
        /** when the APDU response data is not at least two bytes long */
        data class InsufficientResponseData(val responseData: ByteArray) : Error() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is InsufficientResponseData) return false
                return responseData.contentEquals(other.responseData)
            }

            override fun hashCode(): Int = responseData.contentHashCode()
        }
    }

    /** Value for when wildcardShort for expected length encoding is needed */
    const val EXPECTED_LENGTH_WILDCARD_SHORT: Int = 256
    
    /** Value for when wildcardExtended for expected length encoding is needed */
    const val EXPECTED_LENGTH_WILDCARD_EXTENDED: Int = 65536

    /**
     * An APDU response per ISO/IEC 7816-4. It consists of a conditional body and a two byte trailer.
     * This class does not attempt to semantically verify an APDU response.
     * @see ResponseType
     */
    class Response private constructor(
        private val apdu: ByteArray
    ) : ResponseType {

        companion object {
            /** apdu must be at least 2 bytes long */
            private fun check(bytes: ByteArray): Boolean = bytes.size > 1

            /**
             * Initialize APDU response with raw ByteArray.
             *
             * @param data the raw APDU response data
             * @throws Error.InsufficientResponseData if data is less than 2 bytes
             */
            @Throws(Error::class)
            fun create(data: ByteArray): Response {
                if (!check(data)) {
                    throw Error.InsufficientResponseData(data)
                }
                return Response(data)
            }

            /**
             * Convenience initializer for APDU responses that come in three parts
             * @param body response body, may be empty
             * @param sw1 the SW1 command processing byte
             * @param sw2 the SW2 command processing byte
             */
            @Throws(Error::class)
            fun create(body: ByteArray, sw1: UByte, sw2: UByte): Response {
                val data = body + byteArrayOf(sw1.toByte(), sw2.toByte())
                return create(data)
            }

            /** Success response [0x9000] */
            val OK: Response = Response(byteArrayOf(0x90.toByte(), 0x00))
        }

        override val data: ByteArray?
            get() = if (apdu.size > 2) apdu.copyOfRange(0, apdu.size - 2) else null

        override val nr: Int
            get() = if (apdu.size > 2) apdu.size - 2 else 0

        override val sw1: UByte
            get() = apdu[apdu.size - 2].toUByte()

        override val sw2: UByte
            get() = apdu[apdu.size - 1].toUByte()

        override val sw: UShort
            get() = ((sw1.toInt() shl 8) or sw2.toInt()).toUShort()

        /**
         * Returns the raw APDU bytes
         */
        fun toByteArray(): ByteArray = apdu.copyOf()
    }

    /**
     * An APDU Command per ISO/IEC 7816-4.
     * Command APDU encoding options:
     *
     * ```
     *     case 1:  |CLA|INS|P1 |P2 |                                 len = 4
     *     case 2s: |CLA|INS|P1 |P2 |LE |                             len = 5
     *     case 3s: |CLA|INS|P1 |P2 |LC |...BODY...|                  len = 6..260
     *     case 4s: |CLA|INS|P1 |P2 |LC |...BODY...|LE |              len = 7..261
     *     case 2e: |CLA|INS|P1 |P2 |00 |LE1|LE2|                     len = 7
     *     case 3e: |CLA|INS|P1 |P2 |00 |LC1|LC2|...BODY...|          len = 8..65542
     *     case 4e: |CLA|INS|P1 |P2 |00 |LC1|LC2|...BODY...|LE1|LE2|  len =10..65544
     *
     *     LE, LE1, LE2 may be 0x00.
     *     LC must not be 0x00 and LC1|LC2 must not be 0x00|0x00
     * ```
     */
    class Command private constructor(
        private val apdu: ByteArray,
        private val rawNc: Int,
        private val rawNe: Int?,
        private val dataOffset: Int
    ) : CommandType {

        companion object {
            /**
             * Constructs a CommandAPDU from the four header bytes.
             * This is **case 1** in ISO 7816, no command body.
             *
             * @param cla CLA byte
             * @param ins Instruction byte
             * @param p1 P1 byte
             * @param p2 P2 byte
             * @param ne Nr of expected bytes in response. Default: null
             */
            @Throws(Error::class)
            fun create(cla: UByte, ins: UByte, p1: UByte, p2: UByte, ne: Int? = null): Command {
                return create(cla, ins, p1, p2, null, ne)
            }

            /**
             * Constructs a CommandAPDU from the four header bytes, command data,
             * and expected response data length. This is case 4 in ISO 7816,
             * command data and Le present. The value Nc is taken as
             * `dataLength`.
             * If Ne or Nc are zero, the APDU is encoded as case 1, 2, or 3 per ISO 7816.
             *
             * @param cla CLA byte
             * @param ins Instruction byte
             * @param p1 P1 byte
             * @param p2 P2 byte
             * @param data Command data (optional)
             * @param ne Nr of expected bytes in response. Default: null
             */
            @Throws(Error::class)
            fun create(
                cla: UByte,
                ins: UByte,
                p1: UByte,
                p2: UByte,
                data: ByteArray?,
                ne: Int? = null
            ): Command {
                if (ne != null && (ne > EXPECTED_LENGTH_WILDCARD_EXTENDED || ne < 0)) {
                    throw Error.ExpectedResponseLengthOutOfBounds
                }

                if (data != null && data.isNotEmpty()) {
                    val nc = data.size
                    if (nc > 65535) {
                        throw Error.CommandBodyDataTooLarge
                    }

                    val dataOffset: Int
                    val stream = ByteArrayOutputStream()
                    stream.write(header(cla, ins, p1, p2))

                    val le: Int?
                    if (ne != null) {
                        le = ne
                        // case 4s or 4e
                        if (nc <= 255 && ne <= EXPECTED_LENGTH_WILDCARD_SHORT) {
                            // case 4s
                            dataOffset = 5
                            stream.write(encodeDataLengthShort(nc))
                            stream.write(data)
                            stream.write(encodeExpectedLengthShort(ne))
                        } else {
                            // case 4e
                            dataOffset = 7
                            stream.write(encodeDataLengthExtended(nc))
                            stream.write(data)
                            stream.write(encodeExpectedLengthExtended(ne))
                        }
                    } else {
                        // case 3s or 3e
                        le = null
                        if (nc <= 255) {
                            // case 3s
                            dataOffset = 5
                            stream.write(encodeDataLengthShort(nc))
                        } else {
                            // case 3e
                            dataOffset = 7
                            stream.write(encodeDataLengthExtended(nc))
                        }
                        stream.write(data)
                    }
                    return Command(stream.toByteArray(), nc, le, dataOffset)
                } else {
                    // data empty
                    val stream = ByteArrayOutputStream()
                    stream.write(header(cla, ins, p1, p2))

                    if (ne != null) {
                        // case 2s or 2e
                        if (ne <= EXPECTED_LENGTH_WILDCARD_SHORT) {
                            // case 2s
                            // 256 is encoded 0x0
                            stream.write(encodeExpectedLengthShort(ne))
                        } else {
                            // case 2e
                            stream.write(byteArrayOf(0x0))
                            stream.write(encodeExpectedLengthExtended(ne))
                        }
                        return Command(stream.toByteArray(), 0, ne, 0)
                    } else {
                        // case 1
                        return Command(stream.toByteArray(), 0, null, 0)
                    }
                }
            }

            private fun header(cla: UByte, ins: UByte, p1: UByte, p2: UByte): ByteArray {
                return byteArrayOf(cla.toByte(), ins.toByte(), p1.toByte(), p2.toByte())
            }

            private fun encodeExpectedLengthExtended(ne: Int): ByteArray {
                val l1: UByte
                val l2: UByte
                if (ne == EXPECTED_LENGTH_WILDCARD_EXTENDED) { // == 65536
                    l1 = 0u
                    l2 = 0u
                } else {
                    l1 = (ne shr 8).toUByte()
                    l2 = (ne and 0xFF).toUByte()
                }
                return byteArrayOf(l1.toByte(), l2.toByte())
            }

            private fun encodeExpectedLengthShort(ne: Int): ByteArray {
                val len = if (ne != EXPECTED_LENGTH_WILDCARD_SHORT) ne.toUByte() else 0u.toUByte()
                return byteArrayOf(len.toByte())
            }

            private fun encodeDataLengthExtended(nc: Int): ByteArray {
                val l1 = (nc shr 8).toUByte()
                val l2 = (nc and 0xFF).toUByte()
                return byteArrayOf(0x0, l1.toByte(), l2.toByte())
            }

            private fun encodeDataLengthShort(nc: Int): ByteArray {
                return byteArrayOf(nc.toUByte().toByte())
            }
        }

        override val cla: UByte
            get() = apdu[0].toUByte()

        override val ins: UByte
            get() = apdu[1].toUByte()

        override val p1: UByte
            get() = apdu[2].toUByte()

        override val p2: UByte
            get() = apdu[3].toUByte()

        override val nc: Int
            get() = rawNc

        override val ne: Int?
            get() = rawNe

        override val data: ByteArray?
            get() = if (rawNc > 0 && dataOffset > 0) {
                apdu.copyOfRange(dataOffset, dataOffset + rawNc)
            } else null

        override val bytes: ByteArray
            get() = apdu.copyOf()
    }
}
