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

package de.gematik.egk.cardreaderproviderapi.card

/**
 * Answer-to-reset is of Type ByteArray
 */
typealias ATR = ByteArray

/**
 * General card representation
 */
interface CardType {
    /**
     * Card Answer-to-reset configuration
     */
    val atr: ATR

    /**
     * Card supported protocol(s)
     */
    val protocol: Set<CardProtocol>

    /**
     * Open a communication channel to the Card.
     *
     * Note: the basic channel assumes the channel number 0.
     *
     * @throws CardError when failed to connect to the Card.
     *
     * @return The (connected) card channel
     */
    @Throws(CardError::class)
    fun openBasicChannel(): CardChannelType

    /**
     * Open a new logical channel. The channel is opened issuing a MANAGE CHANNEL command that
     * should use the format [0x0, 0x70, 0x0, 0x0, 0x1].
     *
     * @throws CardError when failed to connect to the Card.
     *
     * @return The (connected) card channel
     */
    @Throws(CardError::class)
    suspend fun openLogicChannel(): CardChannelType

    /**
     * Transmit a control command to the Card/Slot
     *
     * Note: implementation is optional.
     *
     * @throws CardError
     *
     * @return The returned ByteArray upon success.
     */
    @Throws(CardError::class)
    fun transmitControl(command: Int, data: ByteArray): ByteArray {
        return ByteArray(0)
    }

    /**
     * Provide an initial application identifier of an application on the underlying card (f.e. the root application).
     * @throws Exception when requesting the application identifier or parsing it.
     * @return The initial application identifier if known, else null.
     */
    @Throws(Exception::class)
    fun initialApplicationIdentifier(): ByteArray? {
        return null
    }

    /**
     * Disconnect connection to the Card.
     *
     * @param reset true to reset the Card after disconnecting.
     *
     * @throws CardError
     */
    @Throws(CardError::class)
    fun disconnect(reset: Boolean)

    /**
     * String description of the card
     */
    val description: String
        get() = "Card[atr=${atr.toHexString()}, protocol=$protocol]"
}

/**
 * Extension to convert ByteArray to hex string
 */
private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
