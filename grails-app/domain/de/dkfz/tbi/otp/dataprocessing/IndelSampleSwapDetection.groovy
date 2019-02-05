/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
