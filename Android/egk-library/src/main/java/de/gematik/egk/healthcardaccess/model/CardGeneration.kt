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

import de.gematik.egk.healthcardaccess.util.toHexString

/**
 * Represent the card generation of health card
 *
 * | Version   | Version with 2 digits | INT Value for Version | Card generation
 * | < 3.0.3   |  03.00.03             | 30003                 | G1
 * | < 4.0.0   |  04.00.00             | 40000                 | G1P
 * | >= 4.0.0  |  04.00.00             | 40000                 | G2
 * | >= 4.4.0  |  04.04.00             | 40400                 | G2_1
 */
enum class CardGeneration {
    /** Generation G1 (< 3.0.3) */
    G1,
    /** Generation G1P (3.0.3 - < 4.0.0) */
    G1P,
    /** Generation G2 (4.0.0 - < 4.4.0) */
    G2,
    /** Generation G2.1 (4.4.0+) */
    G2_1;

    override fun toString(): String = when (this) {
        G1 -> "Gen1"
        G1P -> "Gen1P"
        G2 -> "Gen2"
        G2_1 -> "Gen2.1"
    }

    companion object {
        private const val VERSION_3_0_3 = 30003
        private const val VERSION_4_0_0 = 40000
        private const val VERSION_4_4_0 = 40400

        /** Length in bytes [objectSystemVersion] */
        const val CARD_VERSION_LENGTH = 3

        /**
         * Return the CardGeneration for ObjectSystemVersion
         *
         * @param version value like 30003, 40000 (for details see class description)
         * @return Generation Value or null when version is unrecognized
         */
        fun parseCardGeneration(version: Int): CardGeneration? {
            return when {
                version < 0 -> null
                version < VERSION_3_0_3 -> G1
                version < VERSION_4_0_0 -> G1P
                version < VERSION_4_4_0 -> G2
                else -> G2_1
            }
        }

        /**
         * Parse the CardGeneration from the `objectSystemVersion` from the `CardVersion2`
         */
        fun parseCardGeneration(data: ByteArray): CardGeneration? {
            if (data.size != CARD_VERSION_LENGTH) {
                return null
            }
            val hex = data.toHexString()
            // Radix 10 is not entirely correct here, but we assume version nrs not to grow beyond 99
            // See also Java implementation: de.gematik.ti.healthcard.control.entities.CardGeneration
            val versionInt = hex.toIntOrNull(10) ?: return null
            return parseCardGeneration(versionInt)
        }
    }
}
