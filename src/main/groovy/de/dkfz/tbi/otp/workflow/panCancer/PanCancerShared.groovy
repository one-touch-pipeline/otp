/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

trait PanCancerShared {

    @Autowired
    ConcreteArtefactService concreteArtefactService

    List<SeqTrack> getSeqTracks(WorkflowStep workflowStep) {
        return concreteArtefactService.<SeqTrack> getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ, PanCancerWorkflow.WORKFLOW)
    }

    List<FastqcProcessedFile> getFastqcProcessedFiles(WorkflowStep workflowStep) {
        return concreteArtefactService.<FastqcProcessedFile> getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQC, PanCancerWorkflow.WORKFLOW)
    }

    RoddyBamFile getBaseRoddyBamFile(WorkflowStep workflowStep) {
        return concreteArtefactService.<RoddyBamFile> getInputArtefact(workflowStep,
                PanCancerWorkflow.INPUT_BASE_BAM_FILE, PanCancerWorkflow.WORKFLOW, false)
    }

    RoddyBamFile getRoddyBamFile(WorkflowStep workflowStep) {
        return concreteArtefactService.<RoddyBamFile> getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM, PanCancerWorkflow.WORKFLOW)
    }
}
