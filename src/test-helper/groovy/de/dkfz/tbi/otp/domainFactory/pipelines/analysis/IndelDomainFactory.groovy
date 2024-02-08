/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.domainFactory.pipelines.analysis

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelQualityControl

class IndelDomainFactory extends AbstractAnalysisQcDomainFactory<IndelCallingInstance, IndelQualityControl> {

    static final IndelDomainFactory INSTANCE = new IndelDomainFactory()

    @Override
    protected Class<IndelCallingInstance> getInstanceClass() {
        return IndelCallingInstance
    }

    @Override
    protected Class<IndelQualityControl> getQcClass() {
        return IndelQualityControl
    }

    final String qcFileContent = """\
{
  "all": {
    "file":"/path/to/file",
    "numIndels":23,
    "numIns":24,
    "numDels":25,
    "numSize1_3":26,
    "numSize4_10":27,
    "numSize11plus":28,
    "numInsSize1_3":29,
    "numInsSize4_10":30,
    "numInsSize11plus":31,
    "numDelsSize1_3":32,
    "numDelsSize4_10":33,
    "numDelsSize11plus":34,
    "percentIns":35.0,
    "percentDels":36.0,
    "percentSize1_3":37.0,
    "percentSize4_10":38.0,
    "percentSize11plus":39.0,
    "percentInsSize1_3":40.0,
    "percentInsSize4_10":41.0,
    "percentInsSize11plus":42.0,
    "percentDelsSize1_3":43.0,
    "percentDelsSize4_10":44.0,
    "percentDelsSize11plus":45.0,
    }
}
"""

    @Override
    Map getQcValues() {
        return [
                file                 : '/path/to/file',
                numIndels            : 23,
                numIns               : 24,
                numDels              : 25,
                numSize1_3           : 26,
                numSize4_10          : 27,
                numSize11plus        : 28,
                numInsSize1_3        : 29,
                numInsSize4_10       : 30,
                numInsSize11plus     : 31,
                numDelsSize1_3       : 32,
                numDelsSize4_10      : 33,
                numDelsSize11plus    : 34,
                percentIns           : 35.0,
                percentDels          : 36.0,
                percentSize1_3       : 37.0,
                percentSize4_10      : 38.0,
                percentSize11plus    : 39.0,
                percentInsSize1_3    : 40.0,
                percentInsSize4_10   : 41.0,
                percentInsSize11plus : 42.0,
                percentDelsSize1_3   : 43.0,
                percentDelsSize4_10  : 44.0,
                percentDelsSize11plus: 45.0,
        ]
    }

    final String sampleSwapDetectionFileContent = """\
{
    "somaticSmallVarsInTumorCommonInGnomADPer":1,
    "somaticSmallVarsInControlCommonInGnomad":2,
    "tindaSomaticAfterRescue":3,
    "somaticSmallVarsInControlInBiasPer":4,
    "somaticSmallVarsInTumorPass":5,
    "pid":"pid_1",
    "somaticSmallVarsInControlPass":6,
    "somaticSmallVarsInControlPassPer":7,
    "tindaSomaticAfterRescueMedianAlleleFreqInControl":8.0,
    "somaticSmallVarsInTumorInBiasPer":9.0,
    "somaticSmallVarsInControlCommonInGnomadPer":10,
    "somaticSmallVarsInTumorInBias":11,
    "somaticSmallVarsInControlCommonInGnomasPer":12,
    "germlineSNVsHeterozygousInBothRare":13,
    "germlineSmallVarsHeterozygousInBothRare":14,
    "tindaGermlineRareAfterRescue":15,
    "somaticSmallVarsInTumorCommonInGnomad":16,
    "somaticSmallVarsInControlInBias":17,
    "somaticSmallVarsInControl":18,
    "somaticSmallVarsInTumor":19,
    "germlineSNVsHeterozygousInBoth":20,
    "somaticSmallVarsInTumorPassPer":21.9,
    "somaticSmallVarsInTumorCommonInGnomadPer":22,
    "germlineSmallVarsInBothRare":23,
}
"""
}
