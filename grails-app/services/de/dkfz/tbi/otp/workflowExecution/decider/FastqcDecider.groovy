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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

@Component
@Transactional
@Slf4j
class FastqcDecider implements Decider {

    @Autowired
    ConfigFragmentService configFragmentService

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    FastQcProcessedFileService fastQcProcessedFileService

    @Autowired
    ProcessingPriorityService processingPriorityService

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WorkflowService workflowService

    @Autowired
    SeqTrackService seqTrackService

    @Override
    Collection<WorkflowArtefact> decide(Collection<WorkflowArtefact> inputArtefacts, boolean forceRun = false, Map<String, String> userParams = [:]) {
        final Workflow workflow = workflowService.getExactlyOneWorkflow(BashFastQcWorkflow.WORKFLOW)

        //currently fixed to one supported version, will be changed later
        final WorkflowVersion workflowVersion = CollectionUtils.exactlyOneElement(WorkflowVersion.findAllByWorkflow(workflow))

        return inputArtefacts.collectMany { WorkflowArtefact workflowArtefact ->
            LogUsedTimeUtils.logUsedTime(log, "        decide for: ${workflowArtefact.toString().replaceAll('\n', ', ')}") {
                decideEach(workflowArtefact, workflowVersion)
            }
        }.findAll()
    }

    private List<WorkflowArtefact> decideEach(WorkflowArtefact inputArtefact, WorkflowVersion workflowVersion, boolean forceRun = false) {
        if (inputArtefact.artefactType != ArtefactType.FASTQ) {
            return []
        }

        if (!forceRun &&
                WorkflowRunInputArtefact.findAllByWorkflowArtefact(inputArtefact).any {
                    it.workflowRun.workflow == workflowVersion.workflow
                }) {
            return []
        }

        Optional<Artefact> optionalArtefact = inputArtefact.artefact
        if (!optionalArtefact.isPresent()) {
            return []
        }

        SeqTrack seqTrack = optionalArtefact.get() as SeqTrack
        assert !seqTrack.project.archived
        String workDirectory = fastQcProcessedFileService.buildWorkingPath(workflowVersion)
        List<DataFile> dataFiles = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        Map<DataFile, FastqcProcessedFile> fastqcProcessedFiles = dataFiles.collectEntries {
            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllByDataFile(it)) ?:
                    new FastqcProcessedFile([
                            dataFile         : it,
                            workDirectoryName: workDirectory,
                    ]).save(flush: true)
            [(it): fastqcProcessedFile]
        }

        List<String> runDisplayName = generateWorkflowRunDisplayName(seqTrack)
        List<String> artefactDisplayName = runDisplayName.clone()
        artefactDisplayName.remove(0)
        String shortName = "${BashFastQcWorkflow.WORKFLOW}: ${seqTrack.individual.pid} " +
                "${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                workflowVersion.workflow,
                seqTrack.project.processingPriority,
                fastqcDataFilesService.fastqcOutputDirectory(fastqcProcessedFiles.values().first()).toString(),
                seqTrack.individual.project,
                runDisplayName,
                shortName,
                getConfigFragments(seqTrack, workflowVersion),
        )

        new WorkflowRunInputArtefact(
                workflowRun: run,
                role: BashFastQcWorkflow.INPUT_FASTQ,
                workflowArtefact: inputArtefact,
        ).save(flush: true)

        List<WorkflowArtefact> result = []

        dataFiles.eachWithIndex { DataFile it, int i ->
            WorkflowArtefact workflowArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                    run,
                    "${BashFastQcWorkflow.OUTPUT_FASTQC}_${i + 1}",
                    ArtefactType.FASTQC,
                    artefactDisplayName,
            )).save(flush: true)

            FastqcProcessedFile fastqcProcessedFile = fastqcProcessedFiles[it]
            fastqcProcessedFile.workflowArtefact = workflowArtefact
            fastqcProcessedFile.save(flush: true)
            result << workflowArtefact
        }
        return result
    }

    List<ExternalWorkflowConfigFragment> getConfigFragments(SeqTrack seqTrack, WorkflowVersion workflowVersion) {
        return configFragmentService.getSortedFragments(new SingleSelectSelectorExtendedCriteria(
                workflowVersion.workflow,
                workflowVersion,
                seqTrack.project,
                seqTrack.seqType,
                null, //referenceGenome, not used for FastQC
                seqTrack.libraryPreparationKit,
        ))
    }

    /**
     * Generate display name for the workflowRun.
     *
     * @param seqTrack of the workflowRun
     * @return display name as list of strings
     */
    private List<String> generateWorkflowRunDisplayName(SeqTrack seqTrack) {
        List<String> runDisplayName = []
        runDisplayName.with {
            add("project: ${seqTrack.project.name}")
            add("individual: ${seqTrack.individual.displayName}")
            add("sampleType: ${seqTrack.sampleType.displayName}")
            add("seqType: ${seqTrack.seqType.displayNameWithLibraryLayout}")
            add("run: ${seqTrack.run.name}")
            add("lane: ${seqTrack.laneId}")
        }
        return runDisplayName
    }
}
