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
 * SmartCard communication protocol representation
 */
enum class CardProtocol(val rawValue: UInt) {
    /** T=0 protocol */
    T0(1u shl 0),
    
    /** T=1 protocol */
    T1(1u shl 1),
    
    /** T=15 protocol */
    T15(1u shl 2),
    
    /** T=* protocol (any) */
    ANY(1u shl 3);

    companion object {
        /**
         * Create a set of protocols from a bitmask
         */
        fun fromRawValue(rawValue: UInt): Set<CardProtocol> {
            return entries.filter { (rawValue and it.rawValue) != 0u }.toSet()
        }
        
        /**
         * Convert a set of protocols to a bitmask
         */
        fun toRawValue(protocols: Set<CardProtocol>): UInt {
            return protocols.fold(0u) { acc, protocol -> acc or protocol.rawValue }
        }
    }
}

/**
 * Extension to convert a Set<CardProtocol> to a bitmask
 */
fun Set<CardProtocol>.toRawValue(): UInt = CardProtocol.toRawValue(this)

/**
 * Extension to check if a set contains a specific protocol
 */
fun Set<CardProtocol>.contains(protocol: CardProtocol): Boolean = this.any { it == protocol }
