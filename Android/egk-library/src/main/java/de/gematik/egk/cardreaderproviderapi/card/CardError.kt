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
 * Error cases for the (Smart)Card domain
 */
sealed class CardError : Exception() {
    /**
     * When a particular action is not allowed
     */
    data class SecurityError(override val cause: Throwable? = null) : CardError()

    /**
     * When a connection failed to establish or went away unexpectedly
     */
    data class ConnectionError(override val cause: Throwable? = null) : CardError()

    /**
     * Upon encountering an illegal/unexpected state for a certain action
     */
    data class IllegalState(override val cause: Throwable? = null) : CardError()

    /**
     * General card operation error
     */
    data class OperationError(override val message: String? = null, override val cause: Throwable? = null) : CardError()
}
