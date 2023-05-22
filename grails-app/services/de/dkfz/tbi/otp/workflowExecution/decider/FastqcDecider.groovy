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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactData

@CompileDynamic
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
        deciderResult.infos << "start decider for ${workflowWes} / ${workflowBash}"

        List<FastqcArtefactData<SeqTrack>> seqTrackData = fastqcArtefactService.fetchSeqTrackArtefacts(inputArtefacts)
        List<SeqTrack> seqTracks = seqTrackData*.artefact

        if (seqTracks.empty) {
            String msg = "no data found for ${workflowWes} / ${workflowBash}, skip"
            log.debug("        ${msg}")
            deciderResult.infos << msg
            return deciderResult
        }

        Map<SeqTrack, List<DataFile>> dataFilesMap = fastqcArtefactService.fetchDataFiles(seqTracks)
        List<FastqcArtefactData<FastqcProcessedFile>> fastqcProcessedFileData = fastqcArtefactService.fetchRelatedFastqcArtefactsForSeqTracks(seqTracks)

        Map<Project, WorkflowVersionSelector> workflowVersionSelectorMap =
                LogUsedTimeUtils.logUsedTime(log, "        fetch workflow selectors") {
                    fastqcArtefactService.fetchWorkflowVersionSelectorForSeqTracks(seqTracks, [workflowWes, workflowBash]).collectEntries {
                        assert !it.project.archived
                        [(it.project): it]
                    }
                }

        Map<SeqTrack, List<FastqcArtefactData<FastqcProcessedFile>>> groupedAdditionalDataPerSeqtrack =
                LogUsedTimeUtils.logUsedTime(log, "        group additional Artefacts") {
                    fastqcProcessedFileData.groupBy {
                        it.artefact.dataFile.seqTrack
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
                        deciderResult.warnings << "Fastqc: Ignore ${project}, since no workflow version configured"
                        return
                    }
                    deciderResult.infos << "Fastqc: Use ${matchingWorkflows.workflowVersion} for ${project}"

                    groups.each { FastqcArtefactData<SeqTrack> fastqcArtefactData ->
                        List<FastqcArtefactData<FastqcProcessedFile>> additionalArtefacts = groupedAdditionalDataPerSeqtrack[fastqcArtefactData.artefact]
                        deciderResult.add(createWorkflowRunsAndOutputArtefacts(
                                fastqcArtefactData,
                                additionalArtefacts,
                                dataFilesMap,
                                matchingWorkflows)
                        )
                    }
                }
            }
        }
        deciderResult.infos << "end decider for ${workflowWes} / ${workflowBash}"
        SessionUtils.withTransaction {
            it.flush()
        }
        return deciderResult
    }

    protected DeciderResult createWorkflowRunsAndOutputArtefacts(FastqcArtefactData<SeqTrack> fastqcArtefactData,
                                                               List<FastqcArtefactData<FastqcProcessedFile>> additionalArtefacts,
                                                               Map<SeqTrack, List<DataFile>> dataFilesMap,
                                                               WorkflowVersionSelector matchingWorkflow) {
        DeciderResult deciderResult = new DeciderResult()
        SeqTrack seqTrack = fastqcArtefactData.artefact
        String seqTrackString = seqTrack.toString().replaceAll('<br>', ', ')
        deciderResult.infos << "process seqTrack ${seqTrackString}"

        List<DataFile> dataFiles = dataFilesMap[seqTrack]

        if (additionalArtefacts && additionalArtefacts.size() == dataFiles.size()) {
            deciderResult.warnings << "skip ${seqTrackString}, since fastqc already exist"
            return deciderResult
        }

        Map<DataFile, FastqcProcessedFile> fastqcPerDataFile = additionalArtefacts ? additionalArtefacts.collectEntries {
            [(it.artefact.dataFile): it.artefact]
        } : [:]

        WorkflowVersion workflowVersion = matchingWorkflow.workflowVersion
        Workflow workflow = workflowVersion.workflow

        String workDirectory = fastQcProcessedFileService.buildWorkingPath(workflowVersion)
        Map<DataFile, FastqcProcessedFile> fastqcProcessedFiles = dataFiles.collectEntries {
            FastqcProcessedFile fastqcProcessedFile = fastqcPerDataFile[it] ?: new FastqcProcessedFile([
                    dataFile         : it,
                    workDirectoryName: workDirectory,
            ]).save(flush: false)
            [(it): fastqcProcessedFile]
        }

        List<String> runDisplayName = generateWorkflowRunDisplayName(seqTrack)
        List<String> artefactDisplayName = runDisplayName.clone()
        artefactDisplayName.remove(0)
        String shortName = "${workflow}: ${seqTrack.individual.pid} " +
                "${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                workflow,
                seqTrack.project.processingPriority,
                fastqcDataFilesService.fastqcOutputDirectory(fastqcProcessedFiles.values().first()).toString(),
                seqTrack.individual.project,
                runDisplayName,
                shortName,
                getConfigFragments(seqTrack, workflowVersion),
                workflowVersion,
        )

        new WorkflowRunInputArtefact(
                workflowRun: run,
                role: BashFastQcWorkflow.INPUT_FASTQ,
                workflowArtefact: fastqcArtefactData.workflowArtefact,
        ).save(flush: false)

        List<WorkflowArtefact> result = []

        dataFiles.eachWithIndex { DataFile it, int i ->
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
            deciderResult.infos << "--> create fastqc file ${fastqcProcessedFile.toString().replaceAll('<br>', ', ')}"
            deciderResult.newArtefacts << workflowArtefact
        }
        return deciderResult
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
