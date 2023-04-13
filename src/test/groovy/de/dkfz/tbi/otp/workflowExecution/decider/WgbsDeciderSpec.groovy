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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.workflow.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

class WgbsDeciderSpec extends AbstractAlignmentDeciderSpec {

    void setup() {
        decider = new WgbsDecider()
        useFastqcCount = 0
    }

    void "supportsIncrementalMerging"() {
        expect:
        decider.supportsIncrementalMerging() == false
    }

    void "requiresFastqcResults"() {
        expect:
        decider.requiresFastqcResults() == false
    }

    void "getWorkflowName"() {
        expect:
        decider.workflowName == WgbsWorkflow.WORKFLOW
    }

    void "getInputFastqRole"() {
        expect:
        decider.inputFastqRole == WgbsWorkflow.INPUT_FASTQ
    }

    void "getInputFastqcRole"() {
        expect:
        decider.inputFastqcRole == null
    }

    void "getInputBaseBamRole"() {
        expect:
        decider.inputBaseBamRole == null
    }

    void "getOutputBamRole"() {
        expect:
        decider.outputBamRole == WgbsWorkflow.OUTPUT_BAM
    }

    void "getWorkflow"() {
        given:
        decider.workflowService = new WorkflowService()
        createWorkflow(name: WgbsWorkflow.WORKFLOW)

        expect:
        decider.workflow.name == WgbsWorkflow.WORKFLOW
    }

    void "getSupportedInputArtefactTypes"() {
        expect:
        TestCase.assertContainSame(decider.supportedInputArtefactTypes, [
                ArtefactType.FASTQ,
        ])
    }
}
