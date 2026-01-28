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

package de.gematik.egk.healthcardaccess.filesystem

import de.gematik.egk.healthcardaccess.cardobjects.ApplicationIdentifier
import de.gematik.egk.healthcardaccess.cardobjects.DedicatedFile
import de.gematik.egk.healthcardaccess.cardobjects.ElementaryFile
import de.gematik.egk.healthcardaccess.cardobjects.FileIdentifier
import de.gematik.egk.healthcardaccess.cardobjects.Password
import de.gematik.egk.healthcardaccess.cardobjects.ShortFileIdentifier

/**
 * Card File system layout for EGK smart-cards (elektronische Gesundheitskarte)
 */
object EgkFileSystem {

    /**
     * Elementary File identifiers for EGK
     */
    object EF {
        /** MF/EF.ATR: Transparent Elementary File - Answer to reset */
        val atr = ElementaryFile(FileIdentifier.create("2F01"), ShortFileIdentifier.create("1D"))

        /** MF/EF.CardAccess */
        val cardAccess = ElementaryFile(FileIdentifier.create("011C"), ShortFileIdentifier.create("1C"))

        /** MF/EF.C.CA_eGK.CS.E256 */
        val cCaEgkCsE256 = ElementaryFile(FileIdentifier.create("2F07"), ShortFileIdentifier.create("07"))

        /** MF/EF.C.eGK.AUT_CVC.E256 */
        val cEgkAutCVCE256 = ElementaryFile(FileIdentifier.create("2F06"), ShortFileIdentifier.create("06"))

        /** MF/EF.DIR: Linear variable Elementary File - list application templates */
        val dir = ElementaryFile(FileIdentifier.create("2F00"), ShortFileIdentifier.create("1E"))

        /** MF/EF.GDO: Transparent Elementary File */
        val gdo = ElementaryFile(FileIdentifier.create("2F02"), ShortFileIdentifier.create("02"))

        /** MF/EF.VERSION */
        val version = ElementaryFile(FileIdentifier.create("2F10"), ShortFileIdentifier.create("10"))

        /** MF/EF.VERSION2 */
        val version2 = ElementaryFile(FileIdentifier.create("2F11"), ShortFileIdentifier.create("11"))

        /** MF/DF.NFD.EF.NFD */
        val nfd = ElementaryFile(FileIdentifier.create("D010"), ShortFileIdentifier.create("10"))

        /** MF/DF.HCA.EF.Einwilligung */
        val hcaEinwilligung = ElementaryFile(FileIdentifier.create("D005"), ShortFileIdentifier.create("05"))

        /** MF/DF.HCA.EF.GVD */
        val hcaGVD = ElementaryFile(FileIdentifier.create("D003"), ShortFileIdentifier.create("03"))

        /** MF/DF.HCA.EF.Logging */
        val hcaLogging = ElementaryFile(FileIdentifier.create("D006"), ShortFileIdentifier.create("06"))

        /** MF/DF.HCA.EF.PD */
        val hcaPD = ElementaryFile(FileIdentifier.create("D001"), ShortFileIdentifier.create("01"))

        /** MF/DF.HCA.EF.Prüfungsnachweis */
        val hcaPruefungsnachweis = ElementaryFile(FileIdentifier.create("D01C"), ShortFileIdentifier.create("1C"))

        /** MF/DF.HCA.EF.Standalone */
        val hcaStandalone = ElementaryFile(FileIdentifier.create("DA0A"), ShortFileIdentifier.create("0A"))

        /** MF/DF.HCA.EF.StatusVD */
        val hcaStatusVD = ElementaryFile(FileIdentifier.create("D00C"), ShortFileIdentifier.create("0C"))

        /** MF/DF.HCA.EF.TTN */
        val hcaTTN = ElementaryFile(FileIdentifier.create("D00F"), ShortFileIdentifier.create("0F"))

        /** MF/DF.HCA.EF.VD */
        val hcaVD = ElementaryFile(FileIdentifier.create("D002"), ShortFileIdentifier.create("02"))

        /** MF/DF.HCA.EF.Verweis */
        val hcaVerweis = ElementaryFile(FileIdentifier.create("D009"), ShortFileIdentifier.create("09"))

        /** MF/DF.ESIGN.EF.C.CH.AUT.R2048 */
        val esignCChAutR2048 = ElementaryFile(FileIdentifier.create("C500"), ShortFileIdentifier.create("01"))

        /** MF/DF.ESIGN.EF.C.CH.AUT.E256 */
        val esignCChAutE256 = ElementaryFile(FileIdentifier.create("C504"), ShortFileIdentifier.create("04"))

        /** MF/DF.ESIGN.EF.AUTN */
        val esignCChAutnR2048 = ElementaryFile(FileIdentifier.create("C509"), ShortFileIdentifier.create("09"))

        /** MF/DF.ESIGN.EF.ENC */
        val esignCChEncR2048 = ElementaryFile(FileIdentifier.create("C200"), ShortFileIdentifier.create("02"))

        /** MF/DF.ESIGN.EF.ENCV */
        val esignCChEncvR2048 = ElementaryFile(FileIdentifier.create("C50A"), ShortFileIdentifier.create("0A"))
    }

    /**
     * Dedicated File identifiers for EGK
     */
    object DF {
        /** MF (root) */
        val MF = DedicatedFile(
            aid = ApplicationIdentifier.create("D2760001448000"),
            fid = FileIdentifier.create("3F00")
        )

        /** MF/DF.HCA */
        val HCA = DedicatedFile(aid = ApplicationIdentifier.create("D27600000102"))

        /** MF/DF.ESIGN */
        val ESIGN = DedicatedFile(aid = ApplicationIdentifier.create("A000000167455349474E"))

        /** MF/DF.QES */
        val QES = DedicatedFile(aid = ApplicationIdentifier.create("D27600006601"))

        /** MF/DF.NFD */
        val NFD = DedicatedFile(aid = ApplicationIdentifier.create("D27600014407"))

        /** MF/DF.DPE */
        val DPE = DedicatedFile(aid = ApplicationIdentifier.create("D27600014408"))

        /** MF/DF.GDD */
        val GDD = DedicatedFile(aid = ApplicationIdentifier.create("D2760001440A"))

        /** MF/DF.OSE */
        val OSE = DedicatedFile(aid = ApplicationIdentifier.create("D2760001440B"))

        /** MF/DF.AMTS */
        val AMTS = DedicatedFile(aid = ApplicationIdentifier.create("D2760001440C"))
    }

    /**
     * PIN types for EGK
     */
    enum class Pin(val password: Password) {
        /** PIN CH */
        PIN_CH(Password.create("01")),
        /** MR.PIN.HOME */
        MRPIN_HOME(Password.create("02")),
        /** MR.PIN.NFD */
        MRPIN_NFD(Password.create("03")),
        /** MR.PIN.NFD.READ */
        MRPIN_NFD_READ(Password.create("07")),
        /** MR.PIN.DPE */
        MRPIN_DPE(Password.create("04")),
        /** MR.PIN.DPEREAD */
        MRPIN_DPE_READ(Password.create("08")),
        /** MR.PIN.GDD */
        MRPIN_GDD(Password.create("05")),
        /** MR.PIN.OSE */
        MRPIN_OSE(Password.create("09")),
        /** MR.PIN.AMTS */
        MRPIN_AMTS(Password.create("0C")),
        /** MR.PIN.AMTSREP */
        PIN_AMTS_REP(Password.create("0D")),
        /** MR.PIN.QES */
        PIN_QES(Password.create("01"));
    }
}
