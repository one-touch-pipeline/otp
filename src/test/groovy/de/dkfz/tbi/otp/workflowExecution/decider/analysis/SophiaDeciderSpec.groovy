/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.decider.analysis

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType

class SophiaDeciderSpec extends AbstractAnalysisDeciderNoAnalysisDependencySpec<SophiaInstance> {

    @Override
    Class[] getDomainClassesToMock() {
        return super.domainClassesToMock + [
                SophiaInstance,
        ]
    }

    void setup() {
        decider = new SophiaDecider([
                sophiaWorkFileService: Mock(SophiaWorkFileService) {
                    0 * _
                    _ * constructInstanceName(_) >> "instance"
                },
        ])
    }

    void "getWorkflowName, should return SophiaWorkflow.WORKFLOW"() {
        expect:
        decider.workflowName == SophiaWorkflow.WORKFLOW
    }

    void "getInstanceClass, should return SophiaInstance"() {
        expect:
        decider.instanceClasses == [SophiaInstance]
    }

    void "getDependingAnalysisInstanceClass, should return empty map"() {
        expect:
        decider.dependingAnalysisInstanceClasses == [:]
    }

    void "getArtefactType, should return ArtefactType.SOPHIA"() {
        expect:
        decider.artefactType == ArtefactType.SOPHIA
    }

    void "getPipelineName, should return Pipeline.Name.RODDY_SOPHIA"() {
        expect:
        decider.pipelineName == Pipeline.Name.RODDY_SOPHIA
    }

    @Override
    protected SophiaDomainFactory getFactory() {
        return SophiaDomainFactory.INSTANCE
    }
}
