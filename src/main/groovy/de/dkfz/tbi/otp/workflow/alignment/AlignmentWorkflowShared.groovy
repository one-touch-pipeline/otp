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
package de.dkfz.tbi.otp.workflow.alignment

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.WorkflowShared
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

trait AlignmentWorkflowShared implements WorkflowShared {
    private static final List<String> WORKFLOWS = [PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WORKFLOW, RnaAlignmentWorkflow.WORKFLOW]

    @Autowired
    ConcreteArtefactService concreteArtefactService

    RoddyBamFile getRoddyBamFile(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOWS)
        return concreteArtefactService.<RoddyBamFile> getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM)
    }

    List<SeqTrack> getSeqTracks(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOWS)
        return concreteArtefactService.<SeqTrack> getInputArtefacts(workflowStep, AlignmentWorkflow.INPUT_FASTQ)
    }
}
