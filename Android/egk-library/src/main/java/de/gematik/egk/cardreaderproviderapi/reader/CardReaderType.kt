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

package de.gematik.egk.cardreaderproviderapi.reader

import de.gematik.egk.cardreaderproviderapi.card.CardError
import de.gematik.egk.cardreaderproviderapi.card.CardType

/**
 * General card reader representation.
 *
 * A card reader represents only one (logical) slot, when a CardReader supports multiple
 * cards at the same time it needs to provide a CardReaderType for each slot.
 */
interface CardReaderType {
    /**
     * CardReader name
     */
    val name: String

    /**
     * Returns the system displayable name of this reader.
     */
    val displayName: String
        get() = name

    /**
     * Whether there is a SmartCard present (mute or not) at the time of reading the property
     */
    val cardPresent: Boolean

    /**
     * Set a callback for when a card presence changes
     *
     * @param callback Callback that takes the CardReader as parameter
     */
    fun onCardPresenceChanged(callback: (CardReaderType) -> Unit)

    /**
     * Connect to the currently present SmartCard.
     *
     * @param params Map with arbitrary parameters that might be necessary to connect
     *               the specific reader to a Card/Channel. E.g. NFC Card Reader
     *
     * @throws CardError when the connection could not be established
     *
     * @return instance of the CardType that has been connected or null on mute (there is a card inserted but no
     *         communication with it is possible, e.g. it is inserted upside down)
     */
    @Throws(CardError::class)
    suspend fun connect(params: Map<String, Any> = emptyMap()): CardType?

    /**
     * String description
     */
    val description: String
        get() = "CardReader[$name]"
}
