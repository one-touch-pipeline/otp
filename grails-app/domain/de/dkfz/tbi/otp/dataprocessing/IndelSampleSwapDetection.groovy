package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class IndelSampleSwapDetection implements Entity, QcTrafficLightValue {

    IndelCallingInstance indelCallingInstance

    int somaticSmallVarsInTumorCommonInGnomADPer

    int somaticSmallVarsInControlCommonInGnomad

    int tindaSomaticAfterRescue

    int somaticSmallVarsInControlInBiasPer

    int somaticSmallVarsInTumorPass

    String pid

    int somaticSmallVarsInControlPass

    int somaticSmallVarsInControlPassPer

    double tindaSomaticAfterRescueMedianAlleleFreqInControl

    double somaticSmallVarsInTumorInBiasPer

    int somaticSmallVarsInControlCommonInGnomadPer

    int somaticSmallVarsInTumorInBias

    int somaticSmallVarsInControlCommonInGnomasPer

    int germlineSNVsHeterozygousInBothRare

    int germlineSmallVarsHeterozygousInBothRare

    int tindaGermlineRareAfterRescue

    int somaticSmallVarsInTumorCommonInGnomad

    int somaticSmallVarsInControlInBias

    int somaticSmallVarsInControl

    int somaticSmallVarsInTumor

    int germlineSNVsHeterozygousInBoth

    double somaticSmallVarsInTumorPassPer

    int somaticSmallVarsInTumorCommonInGnomadPer


    static constraints  = {
        indelCallingInstance unique: true
    }
}
