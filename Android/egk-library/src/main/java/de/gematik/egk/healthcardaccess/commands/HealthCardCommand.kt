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

package de.gematik.egk.healthcardaccess.commands

import de.gematik.egk.cardreaderproviderapi.command.APDU
import de.gematik.egk.cardreaderproviderapi.command.CommandType
import de.gematik.egk.healthcardaccess.responses.ResponseStatus

/**
 * HealthCardCommand is a class holding a CommandType object and a dictionary
 * of command context specific ResponseStatuses.
 */
class HealthCardCommand(
    /** CommandType holding the command data */
    val apduCommand: CommandType,
    /** Dictionary mapping from UShort status codes (e.g. 0x9000) to its command context specific ResponseStatuses */
    val responseStatuses: Map<UShort, ResponseStatus>
) : CommandType {

    override val data: ByteArray?
        get() = apduCommand.data

    override val ne: Int?
        get() = apduCommand.ne

    override val nc: Int
        get() = apduCommand.nc

    override val cla: UByte
        get() = apduCommand.cla

    override val ins: UByte
        get() = apduCommand.ins

    override val p1: UByte
        get() = apduCommand.p1

    override val p2: UByte
        get() = apduCommand.p2

    override val bytes: ByteArray
        get() = apduCommand.bytes

    /**
     * Get the ResponseStatus for a given status word
     *
     * @param sw the status word
     * @return the ResponseStatus or null if not found
     */
    fun getResponseStatus(sw: UShort): ResponseStatus? = responseStatuses[sw]
    
    companion object {
        /** Access to PACE commands */
        val PACE = PaceCommands
        
        /** Access to Manage Security Environment commands */
        val ManageSE = ManageSecurityEnvironmentCommands
        
        /** Access to Authentication commands */
        val Authentication = AuthenticationCommands
        
        /** Access to Read commands */
        val Read = ReadCommands
        
        /** Access to Select commands */
        val Select = SelectCommands
    }
}

/**
 * Builder to assemble an instance of HealthCardCommand which is holding a CommandType
 * and a dictionary responseStatuses [UShort: ResponseStatus].
 */
class HealthCardCommandBuilder(
    private var cla: UByte = 0x00u,
    private var ins: UByte = 0x00u,
    private var p1: UByte = 0x00u,
    private var p2: UByte = 0x00u,
    private var data: ByteArray? = null,
    private var ne: Int? = null,
    private var responseStatuses: Map<UShort, ResponseStatus> = emptyMap()
) {

    sealed class InvalidArgument : Exception() {
        data class IllegalSize(val actual: Int, val expected: Int) : InvalidArgument() {
            override val message: String = "Illegal size: $actual, expected: $expected"
        }
        data class IllegalValue(val value: String) : InvalidArgument() {
            override val message: String = "Illegal value: $value"
        }
    }

    /**
     * Constructs a HealthCardCommand from this builder instance
     */
    @Throws(APDU.Error::class)
    fun build(): HealthCardCommand {
        val command = APDU.Command.create(cla, ins, p1, p2, data, ne)
        return HealthCardCommand(command, responseStatuses)
    }

    /**
     * Set CLA byte
     */
    fun setCla(cla: UByte): HealthCardCommandBuilder {
        this.cla = cla
        return this
    }

    /**
     * Set INS byte
     */
    fun setIns(ins: UByte): HealthCardCommandBuilder {
        this.ins = ins
        return this
    }

    /**
     * Set P1 byte
     */
    fun setP1(p1: UByte): HealthCardCommandBuilder {
        this.p1 = p1
        return this
    }

    /**
     * Set P2 byte
     */
    fun setP2(p2: UByte): HealthCardCommandBuilder {
        this.p2 = p2
        return this
    }

    /**
     * Set data
     */
    fun setData(data: ByteArray?): HealthCardCommandBuilder {
        this.data = data
        return this
    }

    /**
     * Append data to existing data
     */
    fun addData(data: ByteArray): HealthCardCommandBuilder {
        this.data = (this.data ?: byteArrayOf()) + data
        return this
    }

    /**
     * Set expected response length (Ne)
     */
    fun setNe(ne: Int?): HealthCardCommandBuilder {
        this.ne = ne
        return this
    }

    /**
     * Set response statuses map
     */
    fun setResponseStatuses(responseStatuses: Map<UShort, ResponseStatus>): HealthCardCommandBuilder {
        this.responseStatuses = responseStatuses
        return this
    }

    companion object {
        /** Marker for setting the first bit (i.e. 0x80) when working with ShortFileIdentifier */
        const val SFID_MARKER: UByte = 0x80u

        /**
         * Deconstruct a given HealthCardCommand back into a builder
         */
        fun fromCommand(command: HealthCardCommand): HealthCardCommandBuilder {
            return HealthCardCommandBuilder(
                cla = command.cla,
                ins = command.ins,
                p1 = command.p1,
                p2 = command.p2,
                data = command.data,
                ne = command.ne,
                responseStatuses = command.responseStatuses
            )
        }
    }
}
