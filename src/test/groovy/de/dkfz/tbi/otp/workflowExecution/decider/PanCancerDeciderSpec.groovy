/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.decider

import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

class PanCancerDeciderSpec extends AbstractAlignmentDeciderSpec {

    void setup() {
        decider = new PanCancerDecider()
        useFastqcCount = 1
    }

    void "supportsIncrementalMerging"() {
        expect:
        decider.supportsIncrementalMerging() == true
    }

    void "requiresFastqcResults"() {
        expect:
        decider.requiresFastqcResults() == true
    }

    void "getWorkflowName"() {
        expect:
        decider.workflowName == PanCancerWorkflow.WORKFLOW
    }

    void "getInputFastqRole"() {
        expect:
        decider.inputFastqRole == PanCancerWorkflow.INPUT_FASTQ
    }

    void "getInputFastqcRole"() {
        expect:
        decider.inputFastqcRole == PanCancerWorkflow.INPUT_FASTQC
    }

    void "getInputBaseBamRole"() {
        expect:
        decider.inputBaseBamRole == PanCancerWorkflow.INPUT_BASE_BAM_FILE
    }

    void "getOutputBamRole"() {
        expect:
        decider.outputBamRole == PanCancerWorkflow.OUTPUT_BAM
    }

    void "getWorkflow"() {
        given:
        decider.workflowService = new WorkflowService()
        createWorkflow(name: PanCancerWorkflow.WORKFLOW)

        expect:
        decider.workflow.name == PanCancerWorkflow.WORKFLOW
    }

    void "getSupportedInputArtefactTypes"() {
        expect:
        TestCase.assertContainSame(decider.supportedInputArtefactTypes, [
                ArtefactType.FASTQ,
                ArtefactType.FASTQC,
                ArtefactType.BAM,
        ])
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, PanCancer cases, when #name, then do not create a new bam file and create a warning"() {
        given:
        createDataForCreateWorkflowRunsAndOutputArtefacts(createMwp, [(key): value])

        and: 'services'
        createEmptyServicesForCreateWorkflowRunsAndOutputArtefacts(mailCount)

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(projectSeqTypeGroup, alignmentDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion)

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.size() == 1
        deciderResult.warnings.first().contains(warningMessagePart)

        where:
        name                       | createMwp | key             | value | mailCount || warningMessagePart
        'missing fastqc'           | false     | 'missingFastqc' | true  | 0         || "since input contains the following fastq without all corresponding fastqc"
        'fastqc without seq track' | false     | 'toManyFastqc'  | true  | 0         || "since input contains the following fastqc without corresponding fastq"
    }
}
