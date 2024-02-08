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

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqQc

class AceseqDomainFactory extends AbstractAnalysisQcDomainFactory<AceseqInstance, AceseqQc> {

    static final AceseqDomainFactory INSTANCE = new AceseqDomainFactory()

    @Override
    protected Class<AceseqInstance> getInstanceClass() {
        return AceseqInstance
    }

    @Override
    protected Class<AceseqQc> getQcClass() {
        return AceseqQc
    }

    final String qcFileContent = """\
{
    "1":{
        "gender":"male",
        "solutionPossible":"3",
        "tcc":"0.5",
        "goodnessOfFit":"0.904231625835189",
        "ploidyFactor":"2.27",
        "ploidy":"2",
    },
    "2":{
        "gender":"female",
        "solutionPossible":"4",
        "tcc":"0.7",
        "goodnessOfFit":"0.12345",
        "ploidyFactor":"1.27",
        "ploidy":"5",
    }
}
"""

    @Override
    Map getQcValues() {
        return qaValuesPropertiesMultipleNumbers[1]
    }

    @SuppressWarnings('DuplicateNumberLiteral')
    Map<Integer, Map> getQaValuesPropertiesMultipleNumbers() {
        return [
                1: [
                        number          : 1,
                        gender          : 'male',
                        solutionPossible: 3,
                        tcc             : 0.5,
                        goodnessOfFit   : 0.904231625835189,
                        ploidyFactor    : 2.27,
                        ploidy          : 2,
                ],
                2: [
                        number          : 2,
                        gender          : 'female',
                        solutionPossible: 4,
                        tcc             : 0.7,
                        goodnessOfFit   : 0.12345,
                        ploidyFactor    : 1.27,
                        ploidy          : 5,
                ],
        ]
    }
}
