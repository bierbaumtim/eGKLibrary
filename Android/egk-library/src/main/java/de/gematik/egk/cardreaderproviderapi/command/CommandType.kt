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

/**
 * SmartCard Application Protocol Data Unit - Command
 */
interface CommandType {
    /**
     * Returns bytes in the command body. If this APDU has no body, this property should return null
     */
    val data: ByteArray?

    /**
     * Returns the maximum number of expected data bytes in a response APDU (Ne/Le).
     * 0 = unlimited/unknown, null = no output expected
     */
    val ne: Int?

    /**
     * Returns the number of data bytes in the command body (Nc) or 0 if this APDU has no body.
     * This call should be equivalent to `data?.size ?: 0`.
     */
    val nc: Int

    /**
     * Returns the value of the class byte CLA.
     */
    val cla: UByte

    /**
     * Returns the value of the instruction byte INS.
     */
    val ins: UByte

    /**
     * Returns the value of the parameter byte P1.
     */
    val p1: UByte

    /**
     * Returns the value of the parameter byte P2.
     */
    val p2: UByte

    /**
     * Serialized APDU message
     */
    val bytes: ByteArray
}

/**
 * Extension function to check equality of CommandType instances
 */
fun CommandType.contentEquals(other: CommandType): Boolean = bytes.contentEquals(other.bytes)
