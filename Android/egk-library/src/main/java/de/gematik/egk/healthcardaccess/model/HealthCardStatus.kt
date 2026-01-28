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

package de.gematik.egk.healthcardaccess.model

/**
 * HealthCard status
 */
sealed class HealthCardStatus {
    /** when card type has not been determined (yet) [e.g. probing] */
    object Unknown : HealthCardStatus()

    /** when card type has been identified by this library */
    data class Valid(val cardType: HealthCardPropertyType?) : HealthCardStatus()

    /** when card type could not be determined */
    object Invalid : HealthCardStatus()

    /** Whether the presented Card is valid in the gematik domain */
    val isValid: Boolean
        get() = this is Valid

    /** The generation version of the card/COS */
    val generation: CardGeneration?
        get() = type?.generation

    /** The kind of gematik Healthcard (eGK, HBA, SMC-B) */
    val type: HealthCardPropertyType?
        get() = (this as? Valid)?.cardType
}
