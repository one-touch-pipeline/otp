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
import de.dkfz.tbi.otp.workflow.shared.*
import de.dkfz.tbi.otp.workflowExecution.*

class FastqcJobService {

    static final String WORKFLOW = "FastQC"
    static final String INPUT_ROLE = "FASTQ"
    static final String OUTPUT_ROLE = "OUTPUT_"

    @Autowired
    LsdfFilesService lsdfFilesService

    SeqTrack getSeqTrack(WorkflowStep workflowStep) {
        WorkflowRun run = workflowStep.workflowRun
        if (run.workflow.name != WORKFLOW) {
            throw new WrongWorkflowException("The step is from workflow ${run.workflow.name}, but expected is ${WORKFLOW}")
        }
        WorkflowArtefact artefact = run.inputArtefacts[INPUT_ROLE]
        if (!artefact) {
            throw new NoArtefactOfRoleException("The WorkflowRun ${run} has no input artefact of role " +
                    "${INPUT_ROLE}, only ${run.inputArtefacts.keySet().sort()}")
        }
        Optional<Artefact> optionalArtefact = artefact.artefact
        if (!optionalArtefact.isPresent()) {
            throw new NoConcreteArtefactException("The WorkflowArtefact ${artefact} of WorkflowRun ${run} has no concrete artefact yet")
        }
        return optionalArtefact.get() as SeqTrack
    }

    List<FastqcProcessedFile> getFastqcProcessedFiles(WorkflowStep workflowStep) {
        WorkflowRun run = workflowStep.workflowRun
        if (run.workflow.name != WORKFLOW) {
            throw new WrongWorkflowException("The step is from workflow ${run.workflow.name}, but expected is ${WORKFLOW}")
        }
        List<WorkflowArtefact> artefact = run.outputArtefacts.findAll {
            it.key.startsWith(OUTPUT_ROLE)
        }*.value

        if (!artefact) {
            throw new NoArtefactOfRoleException("The WorkflowRun ${run} has no output artefact of role " +
                    "${OUTPUT_ROLE}, only ${run.outputArtefacts.keySet().sort()}")
        }
        List<Optional<Artefact>> optionalArtefacts = artefact*.artefact

        optionalArtefacts.each {
            if (!it.isPresent()) {
                throw new NoConcreteArtefactException("The WorkflowArtefact ${it} of WorkflowRun ${run} has no concrete artefact yet")
            }
        }
        return optionalArtefacts.collect {
            it.get() as FastqcProcessedFile
        }
    }
}
