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
package de.dkfz.tbi.otp.dataprocessing.indelcalling

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.AbstractAnalysisResultsService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.FormatHelper

@CompileDynamic
@Transactional
class IndelResultsService extends AbstractAnalysisResultsService<IndelCallingInstance> {

    final Class<IndelCallingInstance> instanceClass = IndelCallingInstance

    @Override
    Map getQcData(IndelCallingInstance analysis) {
        IndelQualityControl qc = CollectionUtils.atMostOneElement(IndelQualityControl.findAllByIndelCallingInstance(analysis))
        IndelSampleSwapDetection sampleSwap = CollectionUtils.atMostOneElement(IndelSampleSwapDetection.findAllByIndelCallingInstance(analysis))
        [
                numIndels: qc?.numIndels ?: "",
                numIns: qc?.numIns ?: "",
                numDels: qc?.numDels ?: "",
                numSize1_3: qc?.numSize1_3 ?: "",
                numSize4_10: qc?.numDelsSize4_10 ?: "",
                germlineSmallVarsInBothRare: sampleSwap?.germlineSmallVarsInBothRare != null ? sampleSwap?.germlineSmallVarsInBothRare : "N/A",
                somaticSmallVarsInTumor: sampleSwap?.somaticSmallVarsInTumor ?: "",
                somaticSmallVarsInControl: sampleSwap?.somaticSmallVarsInControl ?: "",
                somaticSmallVarsInTumorCommonInGnomad: sampleSwap?.somaticSmallVarsInTumorCommonInGnomad ?: "",
                somaticSmallVarsInControlCommonInGnomad: sampleSwap?.somaticSmallVarsInControlCommonInGnomad ?: "",
                somaticSmallVarsInTumorPass: sampleSwap?.somaticSmallVarsInTumorPass ?: "",
                somaticSmallVarsInControlPass: sampleSwap?.somaticSmallVarsInControlPass ?: "",
                tindaSomaticAfterRescue: sampleSwap?.tindaSomaticAfterRescue ?: "",
                tindaSomaticAfterRescueMedianAlleleFreqInControl: sampleSwap ?
                        FormatHelper.formatNumber(sampleSwap.tindaSomaticAfterRescueMedianAlleleFreqInControl) : "",
        ]
    }
}
