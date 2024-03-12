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

import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow

class SophiaDomainFactory extends AbstractAnalysisQcDomainFactory<SophiaInstance, SophiaQc> {

    static final SophiaDomainFactory INSTANCE = new SophiaDomainFactory()

    @Override
    protected String getWorkflowName() {
        return SophiaWorkflow.WORKFLOW
    }

    @Override
    protected Class<? extends AbstractAnalysisWorkflow> getWorkflowClass() {
        return SophiaWorkflow
    }

    @Override
    protected Class<SophiaInstance> getInstanceClass() {
        return SophiaInstance
    }

    @Override
    protected Class<SophiaQc> getQcClass() {
        return SophiaQc
    }

    final String qcFileContent = """\
{
  "all": {
    "controlMassiveInvPrefilteringLevel": 0,
    "tumorMassiveInvFilteringLevel": 0,
    "rnaContaminatedGenesMoreThanTwoIntron": "PRKRA;ACTG2;TYRO3;COL18A1;",
    "rnaContaminatedGenesCount": 4,
    "rnaDecontaminationApplied": false
  }
}
"""

    @Override
    Map getQcValues() {
        return [
                controlMassiveInvPrefilteringLevel   : 0,
                tumorMassiveInvFilteringLevel        : 0,
                rnaContaminatedGenesMoreThanTwoIntron: 'PRKRA;ACTG2;TYRO3;COL18A1;',
                rnaContaminatedGenesCount            : 4,
                rnaDecontaminationApplied            : false,
        ]
    }
}
