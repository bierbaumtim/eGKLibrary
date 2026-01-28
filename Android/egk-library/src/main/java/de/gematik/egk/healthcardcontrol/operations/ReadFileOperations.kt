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

package de.gematik.egk.healthcardcontrol.operations

import de.gematik.egk.healthcardaccess.HealthCardType
import de.gematik.egk.healthcardaccess.cardobjects.DedicatedFile
import de.gematik.egk.healthcardaccess.cardobjects.ShortFileIdentifier
import de.gematik.egk.healthcardaccess.commands.ReadCommands
import de.gematik.egk.healthcardaccess.commands.SelectCommands
import de.gematik.egk.healthcardaccess.responses.ResponseStatus

/**
 * Domain error cases for reading files from a HealthCardType.
 */
sealed class ReadError : Exception() {
    data class UnexpectedResponse(val state: ResponseStatus) : ReadError() {
        override val message = "Unexpected response status: ${state.name}"
    }
    
    data class NoData(val state: ResponseStatus) : ReadError() {
        override val message = "No data received, status: ${state.name}"
    }
    
    data class FcpMissingReadSize(val state: ResponseStatus) : ReadError() {
        override val message = "FCP missing read size, status: ${state.name}"
    }
}

/**
 * Domain error cases for selecting files.
 */
sealed class SelectError : Exception() {
    data class FailedToSelectAid(val aid: ByteArray, val status: ResponseStatus?) : SelectError() {
        override val message = "Failed to select AID: ${aid.toHexString()}, status: $status"
    }
    
    data class FailedToSelectFid(val fid: UShort, val status: ResponseStatus?) : SelectError() {
        override val message = "Failed to select FID: ${fid.toString(16)}, status: $status"
    }
}

/**
 * Extension functions for HealthCardType to read files.
 */

/**
 * Read the current selected DF/EF File.
 * 
 * @param expectedSize The expected file size. Must be greater than 0 or null.
 *                     Note that failOnEndOfFileWarning must be false for this operation
 *                     to succeed when expectedSize is null.
 * @param failOnEndOfFileWarning Whether the operation must execute 'clean' or till
 *                               the end-of-file warning. Default: true
 * @param offset Starting offset for reading. Default: 0
 * 
 * This method keeps reading till the received number of bytes equals expectedSize
 * or the channel returns 0x6282: endOfFileWarning.
 * When the current channel maxResponseLength is less than the expected size,
 * the file is read in chunks and returned as a whole.
 * 
 * @return The data read from the currently selected file
 * @throws ReadError on failure
 */
suspend fun HealthCardType.readSelectedFile(
    expectedSize: Int? = null,
    failOnEndOfFileWarning: Boolean = true,
    offset: Int = 0
): ByteArray {
    val maxResponseLength = currentCardChannel.maxResponseLength - 2 // allow for 2 status bytes sw1, sw2
    val expectedResponseLength = expectedSize ?: 0x10000
    val responseLength = minOf(maxResponseLength, expectedResponseLength)
    
    val readFileCommand = ReadCommands.readFile(ne = responseLength, offset = offset)
    val readFileResponse = transmit(readFileCommand)
    
    val responseStatus = readFileResponse.responseStatus
    if (responseStatus != ResponseStatus.SUCCESS &&
        (failOnEndOfFileWarning || responseStatus != ResponseStatus.END_OF_FILE_WARNING)) {
        throw ReadError.UnexpectedResponse(responseStatus)
    }
    
    val responseData = readFileResponse.data
    if (responseData == null || responseData.isEmpty()) {
        throw ReadError.NoData(responseStatus)
    }
    
    val continueReading = responseData.size < expectedResponseLength &&
            responseStatus != ResponseStatus.END_OF_FILE_WARNING
    
    return if (continueReading) {
        // Continue reading
        val continued = readSelectedFile(
            expectedSize = if (expectedSize != null) expectedResponseLength - responseData.size else null,
            failOnEndOfFileWarning = failOnEndOfFileWarning,
            offset = offset + responseData.size
        )
        responseData + continued
    } else {
        // Done
        responseData
    }
}

/**
 * Select a dedicated file.
 * 
 * @param file The dedicated file to select
 * @return The response status from the select operation
 * @throws SelectError if selection fails
 */
suspend fun HealthCardType.selectDedicated(file: DedicatedFile): ResponseStatus {
    val selectCommand = SelectCommands.selectFile(file.aid)
    val response = transmit(selectCommand)
    
    if (response.responseStatus != ResponseStatus.SUCCESS) {
        throw SelectError.FailedToSelectAid(file.aid.rawValue, response.responseStatus)
    }
    
    return response.responseStatus
}

/**
 * Read a file by selecting it first using its short file identifier.
 * 
 * @param sfid The short file identifier of the file to read
 * @param expectedSize The expected size of the file (null to read until EOF)
 * @return The file contents
 * @throws ReadError on failure
 */
suspend fun HealthCardType.readFile(
    sfid: ShortFileIdentifier,
    expectedSize: Int? = null
): ByteArray {
    val readCommand = ReadCommands.readFileWithSfid(sfid = sfid, ne = expectedSize ?: 0)
    val response = transmit(readCommand)
    
    if (response.responseStatus != ResponseStatus.SUCCESS &&
        response.responseStatus != ResponseStatus.END_OF_FILE_WARNING) {
        throw ReadError.UnexpectedResponse(response.responseStatus)
    }
    
    return response.data ?: throw ReadError.NoData(response.responseStatus)
}

private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
