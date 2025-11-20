/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowShared
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowSharedSpec
import de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class PanCancerSharedSpec extends AlignmentWorkflowSharedSpec<RoddyBamFile> {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                WorkflowRun,
                WorkflowStep,
                RoddyBamFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
        ]
    }

    @Override
    AlignmentWorkflowShared<RoddyBamFile> createSharedInstance() {
        return Spy(PanCancerSharedInstance)
    }

    @Override
    RoddyBamFile createBamFile() {
        return DomainFactory.createRoddyBamFile()
    }

    @Override
    String getWorkflowName() {
        return PanCancerWorkflow.WORKFLOW
    }

    @Override
    String getInputFastqConstant() {
        return PanCancerWorkflow.INPUT_FASTQ
    }

    @Override
    String getOutputBamConstant() {
        return PanCancerWorkflow.OUTPUT_BAM
    }

    void "getFastqcProcessedFiles should call checkWorkflowName and getInputArtefacts with correct arguments"() {
        given:
        PanCancerShared panCancerSharedInstance = Spy(PanCancerSharedInstance)
        panCancerSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([name: PanCancerWorkflow.WORKFLOW])
        ])
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])

        when:
        panCancerSharedInstance.getFastqcProcessedFiles(workflowStep)

        then:
        1 * panCancerSharedInstance.checkWorkflowName(workflowStep, [PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WORKFLOW])

        then:
        1 * panCancerSharedInstance.concreteArtefactService.getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQC) >> _
    }

    @SuppressWarnings('EmptyClass')
    class PanCancerSharedInstance implements PanCancerShared { }
}
