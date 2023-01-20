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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

/**
 * A service providing functionality needed by the different jobs for the data installation workflow.
 */
@CompileDynamic
@Transactional
class DataInstallationInitializationService {

    ConfigFragmentService configFragmentService
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
        List<WorkflowRun> workflowRuns = instance.dataFiles.groupBy {
            it.seqTrack
        }.collect { SeqTrack seqTrack, List<DataFile> dataFiles ->
            createRunForSeqTrack(workflow, seqTrack, dataFiles, priority ?: seqTrack.processingPriority)
        }

        WorkflowRun.withTransaction { status ->
            status.flush()
        }

        return workflowRuns
    }

    private WorkflowRun createRunForSeqTrack(Workflow workflow, SeqTrack seqTrack, List<DataFile> dataFiles, ProcessingPriority priority) {
        List<String> runDisplayName = []
        runDisplayName.with {
            add("project: ${seqTrack.project.name}")
            add("individual: ${seqTrack.individual.displayName}")
            add("sampleType: ${seqTrack.sampleType.displayName}")
            add("seqType: ${seqTrack.seqType.displayNameWithLibraryLayout}")
            add("run: ${seqTrack.run.name}")
            add("lane: ${seqTrack.laneId}")
        }

        List<String> artefactDisplayName = runDisplayName
        artefactDisplayName.remove(0)

        String shortName = "DI: ${seqTrack.individual.pid} ${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"

        String directory = Paths.get(lsdfFilesService.getFileViewByPidPath(dataFiles.first())).parent
        List<ExternalWorkflowConfigFragment> configFragments = getConfigFragments(seqTrack, workflow)

        WorkflowRun run = workflowRunService.buildWorkflowRun(workflow, priority, directory, seqTrack.project, runDisplayName, shortName, configFragments)
        WorkflowArtefact artefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                run, DataInstallationWorkflow.OUTPUT_FASTQ, ArtefactType.FASTQ, artefactDisplayName
        ))
        seqTrack.workflowArtefact = artefact
        seqTrack.save(flush: false)
        return run
    }

    List<ExternalWorkflowConfigFragment> getConfigFragments(SeqTrack seqTrack, Workflow workflow) {
        return configFragmentService.getSortedFragments(new SingleSelectSelectorExtendedCriteria(
                workflow,
                null, //workflowVersion, installation are not versioned
                seqTrack.project,
                seqTrack.seqType,
                null, //referenceGenome, not used for installation
                seqTrack.libraryPreparationKit,
        ))
    }
}
