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

package de.gematik.egk.healthcardaccess.commands

import de.gematik.egk.healthcardaccess.cardobjects.Format2Pin
import de.gematik.egk.healthcardaccess.cardobjects.Password
import de.gematik.egk.healthcardaccess.responses.ResponseStatus

/**
 * These commands represent the User Verification Commands (Benutzerverifikation) in gemSpec_COS#14.6
 */
object UserVerificationCommands {

    private val responseMessages: Map<UShort, ResponseStatus> = mapOf(
        ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
        ResponseStatus.MEMORY_FAILURE.code to ResponseStatus.MEMORY_FAILURE,
        ResponseStatus.WRONG_SECRET_WARNING_COUNT_00.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_00,
        ResponseStatus.WRONG_SECRET_WARNING_COUNT_01.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_01,
        ResponseStatus.WRONG_SECRET_WARNING_COUNT_02.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_02,
        ResponseStatus.WRONG_SECRET_WARNING_COUNT_03.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_03,
        ResponseStatus.SECURITY_STATUS_NOT_SATISFIED.code to ResponseStatus.SECURITY_STATUS_NOT_SATISFIED,
        ResponseStatus.PASSWORD_BLOCKED.code to ResponseStatus.PASSWORD_BLOCKED,
        ResponseStatus.PASSWORD_NOT_USABLE.code to ResponseStatus.PASSWORD_NOT_USABLE,
        ResponseStatus.WRONG_PASSWORD_LENGTH.code to ResponseStatus.WRONG_PASSWORD_LENGTH,
        ResponseStatus.PASSWORD_NOT_FOUND.code to ResponseStatus.PASSWORD_NOT_FOUND
    )

    /**
     * Command representing Change/Set Reference Data Command gemSpec_COS#14.6.1
     */
    object ChangeReferenceData {
        private const val CLA: UByte = 0x00u
        private const val INS: UByte = 0x24u
        private const val PASSWORD_CHANGE: UByte = 0x00u
        private const val PASSWORD_SET: UByte = 0x01u

        /**
         * Use case Change Password Secret (Pin) gemSpec_COS#14.6.1.1
         *
         * @param password The password object to change/update
         * @param dfSpecific whether or not the password object specifies a Global or DF-specific
         * @param oldPin the old secret (pin) to verify
         * @param newPin the new secret (pin) to set
         * @return Command for a change password secret command
         */
        fun changePassword(
            password: Password,
            dfSpecific: Boolean,
            oldPin: Format2Pin,
            newPin: Format2Pin
        ): HealthCardCommand {
            return HealthCardCommandBuilder()
                .setCla(CLA)
                .setIns(INS)
                .setP1(PASSWORD_CHANGE)
                .setP2(password.calculateKeyReference(dfSpecific))
                .setData(oldPin.pin + newPin.pin)
                .setResponseStatuses(responseMessages)
                .build()
        }

        /**
         * Use case Set Password Secret (Pin) gemSpec_COS#14.6.1.2
         *
         * @param password The password object to set
         * @param dfSpecific whether or not the password object specifies a Global or DF-specific
         * @param pin the secret (pin) to set
         * @return Command for a set password secret command
         */
        fun setPassword(
            password: Password,
            dfSpecific: Boolean,
            pin: Format2Pin
        ): HealthCardCommand {
            return HealthCardCommandBuilder()
                .setCla(CLA)
                .setIns(INS)
                .setP1(PASSWORD_SET)
                .setP2(password.calculateKeyReference(dfSpecific))
                .setData(pin.pin)
                .setResponseStatuses(responseMessages)
                .build()
        }
    }

    /**
     * Command representing Verify gemSpec_COS#14.6.5
     */
    object Verify {
        private const val CLA: UByte = 0x00u
        private const val INS: UByte = 0x20u
        private const val P1: UByte = 0x00u

        private val verifyResponseMessages: Map<UShort, ResponseStatus> = mapOf(
            ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_00.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_00,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_01.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_01,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_02.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_02,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_03.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_03,
            ResponseStatus.SECURITY_STATUS_NOT_SATISFIED.code to ResponseStatus.SECURITY_STATUS_NOT_SATISFIED,
            ResponseStatus.PASSWORD_BLOCKED.code to ResponseStatus.PASSWORD_BLOCKED,
            ResponseStatus.TRANSPORT_STATUS_TRANSPORT_PIN.code to ResponseStatus.TRANSPORT_STATUS_TRANSPORT_PIN,
            ResponseStatus.TRANSPORT_STATUS_EMPTY_PIN.code to ResponseStatus.TRANSPORT_STATUS_EMPTY_PIN,
            ResponseStatus.PASSWORD_NOT_FOUND.code to ResponseStatus.PASSWORD_NOT_FOUND
        )

        /**
         * Use case Verify password (PIN) gemSpec_COS#14.6.5.1
         *
         * @param password The password object to verify
         * @param dfSpecific whether or not the password object specifies a Global or DF-specific
         * @param pin the secret (pin) to verify
         * @return Command for a verify password command
         */
        fun verify(
            password: Password,
            dfSpecific: Boolean,
            pin: Format2Pin
        ): HealthCardCommand {
            return HealthCardCommandBuilder()
                .setCla(CLA)
                .setIns(INS)
                .setP1(P1)
                .setP2(password.calculateKeyReference(dfSpecific))
                .setData(pin.pin)
                .setResponseStatuses(verifyResponseMessages)
                .build()
        }
    }

    /**
     * Command representing Get Pin Status Command gemSpec_COS#14.6.4
     */
    object GetPinStatus {
        private const val CLA: UByte = 0x80u
        private const val INS: UByte = 0x20u
        private const val P1: UByte = 0x00u

        private val statusResponseMessages: Map<UShort, ResponseStatus> = mapOf(
            ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
            ResponseStatus.RETRY_COUNTER_COUNT_00.code to ResponseStatus.RETRY_COUNTER_COUNT_00,
            ResponseStatus.RETRY_COUNTER_COUNT_01.code to ResponseStatus.RETRY_COUNTER_COUNT_01,
            ResponseStatus.RETRY_COUNTER_COUNT_02.code to ResponseStatus.RETRY_COUNTER_COUNT_02,
            ResponseStatus.RETRY_COUNTER_COUNT_03.code to ResponseStatus.RETRY_COUNTER_COUNT_03,
            ResponseStatus.TRANSPORT_STATUS_TRANSPORT_PIN.code to ResponseStatus.TRANSPORT_STATUS_TRANSPORT_PIN,
            ResponseStatus.TRANSPORT_STATUS_EMPTY_PIN.code to ResponseStatus.TRANSPORT_STATUS_EMPTY_PIN,
            ResponseStatus.PASSWORD_DISABLED.code to ResponseStatus.PASSWORD_DISABLED,
            ResponseStatus.PASSWORD_NOT_FOUND.code to ResponseStatus.PASSWORD_NOT_FOUND
        )

        /**
         * Use case Get Pin Status gemSpec_COS#14.6.4.1
         *
         * @param password The password object to check
         * @param dfSpecific whether or not the password object specifies a Global or DF-specific
         * @return Command for a get pin status command
         */
        fun getPinStatus(
            password: Password,
            dfSpecific: Boolean
        ): HealthCardCommand {
            return HealthCardCommandBuilder()
                .setCla(CLA)
                .setIns(INS)
                .setP1(P1)
                .setP2(password.calculateKeyReference(dfSpecific))
                .setResponseStatuses(statusResponseMessages)
                .build()
        }
    }

    /**
     * Command representing Reset Retry Counter gemSpec_COS#14.6.6
     */
    object ResetRetryCounter {
        private const val CLA: UByte = 0x00u
        private const val INS: UByte = 0x2Cu

        private val resetResponseMessages: Map<UShort, ResponseStatus> = mapOf(
            ResponseStatus.SUCCESS.code to ResponseStatus.SUCCESS,
            ResponseStatus.MEMORY_FAILURE.code to ResponseStatus.MEMORY_FAILURE,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_00.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_00,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_01.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_01,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_02.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_02,
            ResponseStatus.WRONG_SECRET_WARNING_COUNT_03.code to ResponseStatus.WRONG_SECRET_WARNING_COUNT_03,
            ResponseStatus.COMMAND_BLOCKED.code to ResponseStatus.COMMAND_BLOCKED,
            ResponseStatus.PUK_NOT_FOUND.code to ResponseStatus.PUK_NOT_FOUND
        )

        /**
         * Use case Reset Retry Counter gemSpec_COS#14.6.6.1
         *
         * @param password The password object to reset
         * @param dfSpecific whether or not the password object specifies a Global or DF-specific
         * @param puk the PUK to verify
         * @param newPin the new PIN to set (optional)
         * @return Command for a reset retry counter command
         */
        fun resetRetryCounter(
            password: Password,
            dfSpecific: Boolean,
            puk: Format2Pin,
            newPin: Format2Pin? = null
        ): HealthCardCommand {
            val p1: UByte = if (newPin != null) 0x00u else 0x01u
            val data = if (newPin != null) puk.pin + newPin.pin else puk.pin

            return HealthCardCommandBuilder()
                .setCla(CLA)
                .setIns(INS)
                .setP1(p1)
                .setP2(password.calculateKeyReference(dfSpecific))
                .setData(data)
                .setResponseStatuses(resetResponseMessages)
                .build()
        }
    }
}
