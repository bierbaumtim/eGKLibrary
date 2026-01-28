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

package de.gematik.egk.healthcardaccess.responses

/**
 * Named response statuses per UInt16 status code.
 * Note: Depending on the context a UInt16 status code can have different meanings.
 */
@Suppress("MagicNumber")
enum class ResponseStatus(val code: UShort) {
    // Success
    SUCCESS(0x9000u),
    
    // Exceptions
    UNKNOWN_EXCEPTION(0x6F00u),
    UNKNOWN_STATUS(0x0000u),
    
    // Warnings (0x62xx)
    DATA_TRUNCATED(0x6200u),
    CORRUPT_DATA_WARNING(0x6281u),
    END_OF_FILE_WARNING(0x6282u),
    FILE_DEACTIVATED(0x6282u),
    FILE_TERMINATED(0x6283u),
    RECORD_DEACTIVATED(0x6287u),
    TRANSPORT_STATUS_TRANSPORT_PIN(0x62C1u),
    TRANSPORT_STATUS_EMPTY_PIN(0x62C7u),
    PASSWORD_DISABLED(0x62D0u),
    
    // Authentication failures (0x63xx)
    AUTHENTICATION_FAILURE(0x6300u),
    NO_AUTHENTICATION(0x63CFu),
    
    // Retry counter warnings (0x63Cx)
    RETRY_COUNTER_COUNT_00(0x63C0u),
    RETRY_COUNTER_COUNT_01(0x63C1u),
    RETRY_COUNTER_COUNT_02(0x63C2u),
    RETRY_COUNTER_COUNT_03(0x63C3u),
    RETRY_COUNTER_COUNT_04(0x63C4u),
    RETRY_COUNTER_COUNT_05(0x63C5u),
    RETRY_COUNTER_COUNT_06(0x63C6u),
    RETRY_COUNTER_COUNT_07(0x63C7u),
    RETRY_COUNTER_COUNT_08(0x63C8u),
    RETRY_COUNTER_COUNT_09(0x63C9u),
    RETRY_COUNTER_COUNT_10(0x63CAu),
    RETRY_COUNTER_COUNT_11(0x63CBu),
    RETRY_COUNTER_COUNT_12(0x63CCu),
    RETRY_COUNTER_COUNT_13(0x63CDu),
    RETRY_COUNTER_COUNT_14(0x63CEu),
    RETRY_COUNTER_COUNT_15(0x63CFu),
    
    // Wrong secret warnings (0x63Cx)
    WRONG_SECRET_WARNING_COUNT_00(0x63C0u),
    WRONG_SECRET_WARNING_COUNT_01(0x63C1u),
    WRONG_SECRET_WARNING_COUNT_02(0x63C2u),
    WRONG_SECRET_WARNING_COUNT_03(0x63C3u),
    
    // Execution errors (0x64xx)
    ENCIPHER_ERROR(0x6400u),
    KEY_INVALID(0x6400u),
    OBJECT_TERMINATED(0x6400u),
    PARAMETER_MISMATCH(0x6400u),
    
    // Memory errors (0x65xx)
    MEMORY_FAILURE(0x6581u),
    
    // Wrong length (0x67xx)
    WRONG_RECORD_LENGTH(0x6700u),
    
    // Functions in CLA not supported (0x68xx)
    CHANNEL_CLOSED(0x6881u),
    
    // Command not allowed (0x69xx)
    NO_MORE_CHANNELS_AVAILABLE(0x6981u),
    VOLATILE_KEY_WITHOUT_LCS(0x6981u),
    WRONG_FILE_TYPE(0x6981u),
    SECURITY_STATUS_NOT_SATISFIED(0x6982u),
    COMMAND_BLOCKED(0x6983u),
    KEY_EXPIRED(0x6983u),
    PASSWORD_BLOCKED(0x6983u),
    KEY_ALREADY_PRESENT(0x6985u),
    NO_KEY_REFERENCE(0x6985u),
    NO_PRK_REFERENCE(0x6985u),
    NO_PUK_REFERENCE(0x6985u),
    NO_RANDOM(0x6985u),
    NO_RECORD_LIFE_CYCLE_STATUS(0x6985u),
    PASSWORD_NOT_USABLE(0x6985u),
    WRONG_RANDOM_LENGTH(0x6985u),
    WRONG_RANDOM_OR_NO_KEY_REFERENCE(0x6985u),
    WRONG_PASSWORD_LENGTH(0x6985u),
    NO_CURRENT_EF(0x6986u),
    INCORRECT_SM_DO(0x6988u),
    
    // Wrong parameters (0x6Axx)
    NEW_FILE_SIZE_WRONG(0x6A80u),
    NUMBER_PRECONDITION_WRONG(0x6A80u),
    NUMBER_SCENARIO_WRONG(0x6A80u),
    VERIFICATION_ERROR(0x6A80u),
    WRONG_CIPHER_TEXT(0x6A80u),
    WRONG_TOKEN(0x6A80u),
    UNSUPPORTED_FUNCTION(0x6A81u),
    FILE_NOT_FOUND(0x6A82u),
    RECORD_NOT_FOUND(0x6A83u),
    DATA_TOO_BIG(0x6A84u),
    FULL_RECORD_LIST(0x6A84u),
    MESSAGE_TOO_LONG(0x6A84u),
    OUT_OF_MEMORY(0x6A84u),
    INCONSISTENT_KEY_REFERENCE(0x6A88u),
    WRONG_KEY_REFERENCE(0x6A88u),
    KEY_NOT_FOUND(0x6A88u),
    KEY_OR_PRK_NOT_FOUND(0x6A88u),
    KEY_OR_PWD_NOT_FOUND(0x6A88u),
    PASSWORD_NOT_FOUND(0x6A88u),
    PRK_NOT_FOUND(0x6A88u),
    PUK_NOT_FOUND(0x6A88u),
    DUPLICATED_OBJECTS(0x6A89u),
    DF_NAME_EXISTS(0x6A8Au),
    
    // Wrong P1-P2 (0x6Bxx)
    OFFSET_TOO_BIG(0x6B00u),
    
    // Instruction not supported (0x6Dxx)
    INSTRUCTION_NOT_SUPPORTED(0x6D00u),
    
    // Custom error
    CUSTOM_ERROR(0x0000u);

    companion object {
        /**
         * Get ResponseStatus from SW (status word)
         *
         * @param sw the status word (SW1 << 8 | SW2)
         * @return the matching ResponseStatus or UNKNOWN_STATUS
         */
        fun fromCode(sw: UShort): ResponseStatus {
            return entries.find { it.code == sw } ?: UNKNOWN_STATUS
        }

        /**
         * Get ResponseStatus from SW1 and SW2
         */
        fun fromCode(sw1: UByte, sw2: UByte): ResponseStatus {
            val sw = ((sw1.toInt() shl 8) or sw2.toInt()).toUShort()
            return fromCode(sw)
        }

        /**
         * Check if the status word represents success
         */
        fun isSuccess(sw: UShort): Boolean = sw == SUCCESS.code
    }
}
