/*
 * Copyright 2011-2024 The OTP authors
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
import org.springframework.transaction.TransactionStatus

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactData

@Component
@Transactional
@Slf4j
class FastqcDecider implements Decider {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    FastQcProcessedFileService fastQcProcessedFileService

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WorkflowService workflowService

    @Autowired
    FastqcArtefactService fastqcArtefactService

    @Override
    DeciderResult decide(Collection<WorkflowArtefact> inputArtefacts, Map<String, String> userParams = [:]) {
        DeciderResult deciderResult = new DeciderResult()
        final Workflow workflowWes = workflowService.getExactlyOneWorkflow(WesFastQcWorkflow.WORKFLOW)
        final Workflow workflowBash = workflowService.getExactlyOneWorkflow(BashFastQcWorkflow.WORKFLOW)
        deciderResult.infos << "start decider for ${workflowWes} / ${workflowBash}".toString()

        List<FastqcArtefactData<SeqTrack>> seqTrackData = fastqcArtefactService.fetchSeqTrackArtefacts(inputArtefacts)
        List<SeqTrack> seqTracks = seqTrackData*.artefact

        if (seqTracks.empty) {
            String msg = "no data found for ${workflowWes} / ${workflowBash}, skip"
            log.debug("        ${msg}")
            deciderResult.infos << msg
            return deciderResult
        }

        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFilesMap = fastqcArtefactService.fetchRawSequenceFiles(seqTracks)
        List<FastqcArtefactData<FastqcProcessedFile>> fastqcProcessedFileData = fastqcArtefactService.fetchRelatedFastqcArtefactsForSeqTracks(seqTracks)

        Map<Project, WorkflowVersionSelector> workflowVersionSelectorMap =
                LogUsedTimeUtils.logUsedTime(log, "        fetch workflow selectors") {
                    fastqcArtefactService.fetchWorkflowVersionSelectorForSeqTracks(seqTracks, [workflowWes, workflowBash]).collectEntries {
                        assert it.project.state != Project.State.ARCHIVED
                        assert it.project.state != Project.State.DELETED
                        [(it.project): it]
                    }
                }

        Map<SeqTrack, List<FastqcArtefactData<FastqcProcessedFile>>> groupedAdditionalDataPerSeqtrack =
                LogUsedTimeUtils.logUsedTime(log, "        group additional Artefacts") {
                    fastqcProcessedFileData.groupBy {
                        it.artefact.sequenceFile.seqTrack
                    }
                }

        Map<Project, List<FastqcArtefactData<SeqTrack>>> groupSeqTrackPerProject =
                LogUsedTimeUtils.logUsedTime(log, "        grouped per project") {
                    seqTrackData.groupBy {
                        it.artefact.individual.project
                    }
                }

        LogUsedTimeUtils.logUsedTimeStartEnd(log, "        handle ${groupSeqTrackPerProject.size()} projects") {
            groupSeqTrackPerProject.each { Project project, List<FastqcArtefactData<SeqTrack>> groups ->
                LogUsedTimeUtils.logUsedTimeStartEnd(log, "          handle project ${project} with ${groups.size()} groups") {
                    WorkflowVersionSelector matchingWorkflows = workflowVersionSelectorMap[project]
                    if (!matchingWorkflows) {
                        log.debug("            skip, since no workflow version is configured")
                        deciderResult.warnings << "Fastqc: Ignore ${project}, since no workflow version configured".toString()
                        return
                    }
                    deciderResult.infos << "Fastqc: Use ${matchingWorkflows.workflowVersion} for ${project}".toString()

                    groups.each { FastqcArtefactData<SeqTrack> fastqcArtefactData ->
                        List<FastqcArtefactData<FastqcProcessedFile>> additionalArtefacts = groupedAdditionalDataPerSeqtrack[fastqcArtefactData.artefact]
                        deciderResult.add(createWorkflowRunsAndOutputArtefacts(
                                fastqcArtefactData,
                                additionalArtefacts,
                                rawSequenceFilesMap,
                                matchingWorkflows)
                        )
                    }
                }
            }
        }
        deciderResult.infos << "end decider for ${workflowWes} / ${workflowBash}".toString()
        SessionUtils.withTransaction { TransactionStatus status ->
            status.flush()
        }
        return deciderResult
    }

    protected DeciderResult createWorkflowRunsAndOutputArtefacts(FastqcArtefactData<SeqTrack> fastqcArtefactData,
                                                                 List<FastqcArtefactData<FastqcProcessedFile>> additionalArtefacts,
                                                                 Map<SeqTrack, List<RawSequenceFile>> rawSequenceFilesMap,
                                                                 WorkflowVersionSelector matchingWorkflow) {
        DeciderResult deciderResult = new DeciderResult()
        SeqTrack seqTrack = fastqcArtefactData.artefact
        String seqTrackString = seqTrack.toString().replaceAll('<br>', ', ')
        deciderResult.infos << "process seqTrack ${seqTrackString}".toString()

        List<RawSequenceFile> rawSequenceFiles = rawSequenceFilesMap[seqTrack]

        if (additionalArtefacts && additionalArtefacts.size() == rawSequenceFiles.size()) {
            deciderResult.warnings << "skip ${seqTrackString}, since fastqc already exist".toString()
            return deciderResult
        }

        Map<RawSequenceFile, FastqcProcessedFile> fastqcPerRawSequenceFile = additionalArtefacts ? additionalArtefacts.collectEntries {
            [(it.artefact.sequenceFile): it.artefact]
        } : [:]

        WorkflowVersion workflowVersion = matchingWorkflow.workflowVersion
        Workflow workflow = workflowVersion.workflow

        String workDirectory = fastQcProcessedFileService.buildWorkingPath(workflowVersion)
        Map<RawSequenceFile, FastqcProcessedFile> fastqcProcessedFiles = rawSequenceFiles.collectEntries {
            FastqcProcessedFile fastqcProcessedFile = fastqcPerRawSequenceFile[it] ?: new FastqcProcessedFile([
                    sequenceFile     : it,
                    workDirectoryName: workDirectory,
            ]).save(flush: false)
            [(it): fastqcProcessedFile]
        }

        boolean useUuid = fastQcProcessedFileService.useUuid(workflowVersion)

        List<String> runDisplayName = generateWorkflowRunDisplayName(seqTrack)
        List<String> artefactDisplayName = new ArrayList<>(runDisplayName)
        artefactDisplayName.remove(0)
        String shortName = "${workflow}: ${seqTrack.individual.pid} " +
                "${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                workflow,
                seqTrack.project.processingPriority,
                useUuid ? null : fastqcDataFilesService.fastqcOutputDirectory(fastqcProcessedFiles.values().first()).toString(),
                seqTrack.individual.project,
                runDisplayName,
                shortName,
                workflowVersion,
        )

        new WorkflowRunInputArtefact(
                workflowRun: run,
                role: BashFastQcWorkflow.INPUT_FASTQ,
                workflowArtefact: fastqcArtefactData.workflowArtefact,
        ).save(flush: false)

        List<WorkflowArtefact> result = []

        rawSequenceFiles.eachWithIndex { RawSequenceFile it, int i ->
            WorkflowArtefact workflowArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                    run,
                    "${BashFastQcWorkflow.OUTPUT_FASTQC}_${i + 1}",
                    ArtefactType.FASTQC,
                    artefactDisplayName,
            )).save(flush: false)

            FastqcProcessedFile fastqcProcessedFile = fastqcProcessedFiles[it]
            fastqcProcessedFile.workflowArtefact = workflowArtefact
            fastqcProcessedFile.save(flush: true)
            result << workflowArtefact
            deciderResult.infos << "--> create fastqc file ${fastqcProcessedFile.toString().replaceAll('<br>', ', ')}".toString()
            deciderResult.newArtefacts << workflowArtefact
        }
        return deciderResult
    }

    /**
     * Generate display name for the workflowRun.
     *
     * @param seqTrack of the workflowRun
     * @return display name as list of strings
     */
    private List<String> generateWorkflowRunDisplayName(SeqTrack seqTrack) {
        List<String> runDisplayName = [
                "project: ${seqTrack.project.name}",
                "individual: ${seqTrack.individual.displayName}",
                "sampleType: ${seqTrack.sampleType.displayName}",
                "seqType: ${seqTrack.seqType.displayNameWithLibraryLayout}",
                "run: ${seqTrack.run.name}",
                "lane: ${seqTrack.laneId}",
        ]*.toString()
        return runDisplayName
    }
}
