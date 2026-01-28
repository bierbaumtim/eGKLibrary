// Copyright (c) 2024 gematik GmbH
// 
// Licensed under the Apache License, Version 2.0 (the License);
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an AS IS BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.gematik.egk.healthcardaccess.responses

import de.gematik.egk.cardreaderproviderapi.command.ResponseType

/**
 * Response from a health card command containing the response data and status.
 */
data class HealthCardResponse(
    /** The raw response data (excluding SW1 and SW2) */
    val data: ByteArray?,
    /** Status byte 1 */
    val sw1: Byte,
    /** Status byte 2 */
    val sw2: Byte
) {
    /** Combined status word (SW1 << 8 | SW2) */
    val sw: Int
        get() = ((sw1.toInt() and 0xFF) shl 8) or (sw2.toInt() and 0xFF)
    
    /** Parsed response status */
    val responseStatus: ResponseStatus
        get() = ResponseStatus.fromSW(sw1, sw2)
    
    companion object {
        /**
         * Create a HealthCardResponse from a ResponseType.
         */
        fun fromResponse(response: ResponseType): HealthCardResponse {
            return HealthCardResponse(
                data = response.data,
                sw1 = response.sw1,
                sw2 = response.sw2
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HealthCardResponse) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (sw1 != other.sw1) return false
        if (sw2 != other.sw2) return false
        return true
    }
    
    override fun hashCode(): Int {
        var result = data?.contentHashCode() ?: 0
        result = 31 * result + sw1
        result = 31 * result + sw2
        return result
    }
}
