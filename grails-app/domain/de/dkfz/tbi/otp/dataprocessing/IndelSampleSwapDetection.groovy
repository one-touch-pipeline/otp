package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue
import de.dkfz.tbi.otp.utils.Entity

class IndelSampleSwapDetection implements Entity, QcTrafficLightValue {

    IndelCallingInstance indelCallingInstance

    @QcThresholdEvaluated
    int somaticSmallVarsInTumorCommonInGnomADPer

    @QcThresholdEvaluated
    int somaticSmallVarsInControlCommonInGnomad

    @QcThresholdEvaluated
    int tindaSomaticAfterRescue

    @QcThresholdEvaluated
    int somaticSmallVarsInControlInBiasPer

    @QcThresholdEvaluated
    int somaticSmallVarsInTumorPass

    @QcThresholdEvaluated
    String pid

    @QcThresholdEvaluated
    int somaticSmallVarsInControlPass

    @QcThresholdEvaluated
    int somaticSmallVarsInControlPassPer

    @QcThresholdEvaluated
    double tindaSomaticAfterRescueMedianAlleleFreqInControl

    @QcThresholdEvaluated
    double somaticSmallVarsInTumorInBiasPer

    @QcThresholdEvaluated
    int somaticSmallVarsInControlCommonInGnomadPer

    @QcThresholdEvaluated
    int somaticSmallVarsInTumorInBias

    @QcThresholdEvaluated
    int somaticSmallVarsInControlCommonInGnomasPer

    @QcThresholdEvaluated
    int germlineSNVsHeterozygousInBothRare

    @QcThresholdEvaluated
    int germlineSmallVarsHeterozygousInBothRare

    @QcThresholdEvaluated
    int tindaGermlineRareAfterRescue

    @QcThresholdEvaluated
    int somaticSmallVarsInTumorCommonInGnomad

    @QcThresholdEvaluated
    int somaticSmallVarsInControlInBias

    @QcThresholdEvaluated
    int somaticSmallVarsInControl

    @QcThresholdEvaluated
    int somaticSmallVarsInTumor

    @QcThresholdEvaluated
    int germlineSNVsHeterozygousInBoth

    @QcThresholdEvaluated
    double somaticSmallVarsInTumorPassPer

    @QcThresholdEvaluated
    int somaticSmallVarsInTumorCommonInGnomadPer


    static constraints  = {
        indelCallingInstance unique: true
    }
}
