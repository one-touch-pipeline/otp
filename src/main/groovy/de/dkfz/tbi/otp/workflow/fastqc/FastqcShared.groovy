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
package de.dkfz.tbi.otp.workflow.fastqc

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.WorkflowShared
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

trait FastqcShared extends WorkflowShared {

    static final List<String> ALLOWED_WORKFLOWS = [
            BashFastQcWorkflow.WORKFLOW,
            WesFastQcWorkflow.WORKFLOW,
    ]

    static final String INPUT_ROLE = BashFastQcWorkflow.INPUT_FASTQ
    static final String OUTPUT_ROLE = BashFastQcWorkflow.OUTPUT_FASTQC

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConcreteArtefactService concreteArtefactService

    SeqTrack getSeqTrack(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, ALLOWED_WORKFLOWS)
        return concreteArtefactService.<SeqTrack> getInputArtefact(workflowStep, INPUT_ROLE)
    }

    List<FastqcProcessedFile> getFastqcProcessedFiles(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, ALLOWED_WORKFLOWS)
        return concreteArtefactService.<FastqcProcessedFile> getOutputArtefacts(workflowStep, OUTPUT_ROLE)
    }
}
