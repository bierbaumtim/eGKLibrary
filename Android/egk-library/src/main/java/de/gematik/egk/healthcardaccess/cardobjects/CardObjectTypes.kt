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

package de.gematik.egk.healthcardaccess.cardobjects

/**
 * Card item marker interface
 */
interface CardItemType

/**
 * Card object identifier interface
 * The representation for the Card Object Identifier (E.g. AID, FID)
 */
interface CardObjectIdentifierType {
    /**
     * The raw value representation for the identifier
     */
    val rawValue: ByteArray
}

/**
 * Marker for password - DF specific marker value
 */
const val DF_SPECIFIC_PWD_MARKER: UByte = 0x80u

/**
 * Key reference interface
 */
interface CardKeyReferenceType {
    /**
     * Calculate a key reference
     *
     * @param dfSpecific whether the key should be DF specific
     * @return the calculated key reference
     */
    fun calculateKeyReference(dfSpecific: Boolean): UByte
}
