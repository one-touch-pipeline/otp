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
package de.dkfz.tbi.otp.workflow.datainstallation

import grails.gorm.transactions.Transactional
import org.springframework.transaction.TransactionStatus

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

/**
 * A service providing functionality needed by the different jobs for the data installation workflow.
 */
@Transactional
class DataInstallationInitializationService {

    LsdfFilesService lsdfFilesService
    WorkflowArtefactService workflowArtefactService
    WorkflowRunService workflowRunService
    WorkflowService workflowService

    /**
     * Create for all {@link SeqTrack}s of the {@link FastqImportInstance} a {@link WorkflowRun} and an output {@link WorkflowArtefact}
     * with role {@link #{DataInstallationWorkflow.WORKFLOW}} connecting the {@link WorkflowRun} with the {@link SeqTrack}.
     */
    List<WorkflowRun> createWorkflowRuns(FastqImportInstance instance, ProcessingPriority priority = null) {
        Workflow workflow = workflowService.getExactlyOneWorkflow(DataInstallationWorkflow.WORKFLOW)
        List<WorkflowRun> workflowRuns = instance.sequenceFiles.groupBy {
            it.seqTrack
        }.collect { SeqTrack seqTrack, List<RawSequenceFile> rawSequenceFiles ->
            createRunForSeqTrack(workflow, seqTrack, rawSequenceFiles, priority ?: seqTrack.processingPriority)
        }

        WorkflowRun.withTransaction { TransactionStatus status ->
            status.flush()
        }

        return workflowRuns
    }

    private WorkflowRun createRunForSeqTrack(Workflow workflow, SeqTrack seqTrack, List<RawSequenceFile> rawSequenceFiles, ProcessingPriority priority) {
        List<String> runDisplayName = [
                "project: ${seqTrack.project.name}",
                "individual: ${seqTrack.individual.displayName}",
                "sampleType: ${seqTrack.sampleType.displayName}",
                "seqType: ${seqTrack.seqType.displayNameWithLibraryLayout}",
                "run: ${seqTrack.run.name}",
                "lane: ${seqTrack.laneId}",
        ]*.toString()

        List<String> artefactDisplayName = new ArrayList<>(runDisplayName)
        artefactDisplayName.remove(0)

        String shortName = "DI: ${seqTrack.individual.pid} ${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"

        String directory = Paths.get(lsdfFilesService.getFileViewByPidPath(rawSequenceFiles.first())).parent

        WorkflowRun run = workflowRunService.buildWorkflowRun(workflow, priority, directory, seqTrack.project, runDisplayName, shortName)
        WorkflowArtefact artefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                run, DataInstallationWorkflow.OUTPUT_FASTQ, ArtefactType.FASTQ, artefactDisplayName
        ))
        seqTrack.workflowArtefact = artefact
        seqTrack.save(flush: false)
        return run
    }
}
