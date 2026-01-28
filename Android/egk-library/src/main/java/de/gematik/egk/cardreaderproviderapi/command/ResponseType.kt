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
 * SmartCard Application Protocol Data Unit - Response
 */
interface ResponseType {
    /**
     * Returns bytes in the response body. If this APDU has no body, this method returns null
     */
    val data: ByteArray?

    /**
     * Returns the number of data bytes in the response body (Nr) or 0 if this APDU has no body.
     * This call should be equivalent to `data?.size ?: 0`.
     */
    val nr: Int

    /**
     * Returns the value of the status byte SW1 as a value between 0 and 255.
     */
    val sw1: UByte

    /**
     * Returns the value of the status byte SW2 as a value between 0 and 255.
     */
    val sw2: UByte

    /**
     * Returns the value of the status bytes SW1 and SW2 as a single status word SW.
     */
    val sw: UShort
}

/**
 * Extension function to check equality of ResponseType instances
 */
fun ResponseType.contentEquals(other: ResponseType): Boolean =
    data.contentEquals(other.data) && sw == other.sw

private fun ByteArray?.contentEquals(other: ByteArray?): Boolean {
    return when {
        this == null && other == null -> true
        this == null || other == null -> false
        else -> this.contentEquals(other)
    }
}
