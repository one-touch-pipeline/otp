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
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

class RnaAlignmentDeciderSpec extends AbstractAlignmentDeciderSpec {

    void setup() {
        decider = new RnaAlignmentDecider()
        useFastqcCount = 0
    }

    void "requiresFastqcResults"() {
        expect:
        !decider.requiresFastqcResults()
    }

    void "getWorkflowName"() {
        expect:
        decider.workflowName == RnaAlignmentWorkflow.WORKFLOW
    }

    void "getInputFastqRole"() {
        expect:
        decider.inputFastqRole == RnaAlignmentWorkflow.INPUT_FASTQ
    }

    void "getInputFastqcRole"() {
        expect:
        decider.inputFastqcRole == null
    }

    void "getOutputBamRole"() {
        expect:
        decider.outputBamRole == RnaAlignmentWorkflow.OUTPUT_BAM
    }

    void "getWorkflow"() {
        given:
        decider.workflowService = new WorkflowService()
        createWorkflow(name: RnaAlignmentWorkflow.WORKFLOW)

        expect:
        decider.workflow.name == RnaAlignmentWorkflow.WORKFLOW
    }

    void "getSupportedInputArtefactTypes"() {
        expect:
        TestCase.assertContainSame(decider.supportedInputArtefactTypes, [
                ArtefactType.FASTQ,
        ])
    }
}
