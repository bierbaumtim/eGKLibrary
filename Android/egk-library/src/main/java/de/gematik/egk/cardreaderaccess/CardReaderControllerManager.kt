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

package de.gematik.egk.cardreaderaccess

import de.gematik.egk.cardreaderproviderapi.provider.CardReaderProviderType
import de.gematik.egk.cardreaderproviderapi.provider.ProviderDescriptor
import de.gematik.egk.cardreaderproviderapi.reader.CardReaderControllerType
import java.util.ServiceLoader

/**
 * The interface represents the behavior for a ServiceLoader that provides CardReaderControllerTypes
 */
interface CardReaderControllerManagerType {
    /**
     * A list with card reader controllers for all card reader providers
     */
    val cardReaderControllers: List<CardReaderControllerType>

    /**
     * A list with all card reader provider descriptors found
     */
    val cardReaderProviderDescriptors: List<ProviderDescriptor>

    /**
     * A list of all names of each card reader provider available
     */
    val cardReaderProviderNames: List<String>
        get() = cardReaderProviderDescriptors.map { it.name }
}

/**
 * The CardReaderControllerManager acts as a typical Java ServiceLoader for loading CardReaderControllerType
 * via providers that implement the CardReaderProviderType interface.
 */
class CardReaderControllerManager private constructor(
    private val providers: List<CardReaderProviderType>
) : CardReaderControllerManagerType {

    companion object {
        /**
         * The main/shared instance for general purpose use.
         */
        val shared: CardReaderControllerManagerType by lazy {
            CardReaderControllerManager(loadCardReaderProviders())
        }

        private fun loadCardReaderProviders(): List<CardReaderProviderType> {
            return ServiceLoader.load(CardReaderProviderType::class.java).toList()
        }

        /**
         * Create a manager with explicit providers (for testing purposes)
         */
        fun create(providers: List<CardReaderProviderType>): CardReaderControllerManagerType {
            return CardReaderControllerManager(providers)
        }
    }

    /**
     * Lazy initialize CardReaderControllers
     */
    override val cardReaderControllers: List<CardReaderControllerType> by lazy {
        providers.map { it.provideCardReaderController() }
    }

    /**
     * A list of all complete Provider Descriptors of each card reader provider available
     */
    override val cardReaderProviderDescriptors: List<ProviderDescriptor>
        get() = providers.map { it.descriptor }
}
