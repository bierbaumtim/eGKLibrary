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

package de.gematik.egk.cardreaderproviderapi.provider

import de.gematik.egk.cardreaderproviderapi.reader.CardReaderControllerType

/**
 * The CardReaderProviderType interface serves as the gateway for third-party
 * CardReaderController adapters/drivers to register their implementations for CardReaderController(s)
 * and/or CardReaderProvider(s) within the HealthCardAccess domain.
 */
interface CardReaderProviderType {
    /**
     * Provide the CardReaderController
     *
     * @return The CardReaderControllerType instance
     */
    fun provideCardReaderController(): CardReaderControllerType

    /**
     * Card Reader Provider information
     */
    val descriptor: ProviderDescriptor
}
