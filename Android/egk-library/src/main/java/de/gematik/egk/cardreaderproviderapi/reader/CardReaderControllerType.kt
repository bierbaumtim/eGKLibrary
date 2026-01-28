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

/**
 * Delegate interface for the CardReaderController
 */
interface CardReaderControllerDelegate {
    /**
     * Inform the delegate of a (new) connected/available CardReaderType.
     *
     * @param controller the calling (owning) controller
     * @param cardReader the card reader that became available
     */
    fun onCardReaderConnected(controller: CardReaderControllerType, cardReader: CardReaderType)

    /**
     * Inform the delegate of a card reader disconnect.
     *
     * @param controller the calling (owning) controller
     * @param cardReader the terminal that became unavailable
     */
    fun onCardReaderDisconnected(controller: CardReaderControllerType, cardReader: CardReaderType)
}

/**
 * Controller representation for managing card readers
 */
interface CardReaderControllerType {
    /**
     * The identifier name for the controller
     */
    val name: String

    /**
     * The currently available card readers
     */
    val cardReaders: List<CardReaderType>

    /**
     * Add a delegate to get informed when the cardReaders list changes.
     *
     * @param delegate The delegate that should be added and informed upon updates.
     */
    fun addDelegate(delegate: CardReaderControllerDelegate)

    /**
     * Remove a previously added delegate.
     *
     * @param delegate The delegate that should be removed from receiving updates.
     */
    fun removeDelegate(delegate: CardReaderControllerDelegate)
}
