/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.datainstallation

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.shared.NoArtefactOfRoleException
import de.dkfz.tbi.otp.workflow.shared.NoConcreteArtefactException
import de.dkfz.tbi.otp.workflow.shared.WrongWorkflowException
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

@Transactional
class DataInstallationInitializationService {

    static final String WORKFLOW = "FASTQ installation"

    static final String OUTPUT_ROLE = "FASTQ"

    LsdfFilesService lsdfFilesService

    WorkflowArtefactService workflowArtefactService

    WorkflowRunService workflowRunService

    /**
     * Create for all {@link SeqTrack}s of the {@link FastqImportInstance} a {@link WorkflowRun} and an output {@link WorkflowArtefact}
     * with role {@link #OUTPUT_ROLE} connecting the {@link WorkflowRun} with the {@link SeqTrack}.
     *
     * The returned {@link WorkflowRun}s are saved, but not flushed yet.
     */
    List<WorkflowRun> createWorkflowRuns(FastqImportInstance instance, ProcessingPriority priority = null) {
        Workflow workflow = Workflow.getExactlyOneWorkflow(WORKFLOW)
        return instance.dataFiles.groupBy {
            it.seqTrack
        }.collect { SeqTrack seqTrack, List<DataFile> dataFiles ->
            createRunForSeqTrack(workflow, seqTrack, dataFiles, priority ?: seqTrack.processingPriority)
        }
    }

    private WorkflowRun createRunForSeqTrack(Workflow workflow, SeqTrack seqTrack, List<DataFile> dataFiles, ProcessingPriority priority) {
        String directory = Paths.get(lsdfFilesService.getFileViewByPidPath(dataFiles.first())).parent
        WorkflowRun run = workflowRunService.createWorkflowRun(workflow, priority, directory)
        WorkflowArtefact artefact = workflowArtefactService.createWorkflowArtefact(run, OUTPUT_ROLE, seqTrack.individual, seqTrack.seqType)
        seqTrack.workflowArtefact = artefact
        seqTrack.save(flush: false)
        return run
    }

    SeqTrack getSeqTrack(WorkflowStep workflowStep) {
        WorkflowRun run = workflowStep.workflowRun
        if (run.workflow.name != DataInstallationInitializationService.WORKFLOW) {
            throw new WrongWorkflowException("The step is from workflow ${run.workflow.name}, but expected is ${WORKFLOW}")
        }
        WorkflowArtefact artefact = run.outputArtefacts[DataInstallationInitializationService.OUTPUT_ROLE]
        if (!artefact) {
            throw new NoArtefactOfRoleException("The WorkflowRun ${run} has no output artefact of role " +
                    "${DataInstallationInitializationService.OUTPUT_ROLE}, only ${run.outputArtefacts.keySet().sort()}")
        }
        Optional<Artefact> optionalArtefact = artefact.artefact
        if (!optionalArtefact.isPresent()) {
            throw new NoConcreteArtefactException("The WorkflowArtefact ${artefact} of WorkflowRun ${run} has no concreate artefact yet")
        }
        return optionalArtefact.get()
    }
}
