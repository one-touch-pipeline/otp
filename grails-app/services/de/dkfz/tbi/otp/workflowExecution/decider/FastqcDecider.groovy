/*
 * Copyright 2011-2026 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflow.shared.DeciderCreateWorkflowActionException
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactDataWithFastqcProcessedFile
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactDataWithSeqTrack

@Component
@Transactional
@Slf4j
class FastqcDecider implements Decider {

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WorkflowService workflowService

    @Autowired
    FastqcArtefactService fastqcArtefactService

    @Override
    DeciderResult decide(Collection<WorkflowArtefact> inputArtefacts, Map<String, String> userParams = [:],
                         Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction) {
        DeciderResult deciderResult = new DeciderResult()
        final Workflow workflowWes = workflowService.getExactlyOneWorkflow(WesFastQcWorkflow.WORKFLOW)
        final Workflow workflowBash = workflowService.getExactlyOneWorkflow(BashFastQcWorkflow.WORKFLOW)
        deciderResult.infos << "start decider for ${workflowWes} / ${workflowBash}".toString()
        if (deciderAction[getClass()] == DeciderCreateWorkflowAction.SKIP) {
            String msg = "Skipping creating runs for ${workflowWes} / ${workflowBash}"
            log.debug("        ${msg}")
            deciderResult.infos << msg
            return deciderResult
        }
        List<FastqcArtefactDataWithSeqTrack> seqTrackData = fastqcArtefactService.fetchSeqTrackArtefacts(inputArtefacts)
        List<SeqTrack> seqTracks = seqTrackData*.artefact

        if (seqTracks.empty) {
            String msg = "no data found for ${workflowWes} / ${workflowBash}, skip"
            log.debug("        ${msg}")
            deciderResult.infos << msg
            return deciderResult
        }

        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFilesMap = fastqcArtefactService.fetchRawSequenceFiles(seqTracks)
        List<FastqcArtefactDataWithFastqcProcessedFile> fastqcProcessedFileData = fastqcArtefactService.fetchRelatedFastqcArtefactsForSeqTracks(seqTracks)

        Map<Project, WorkflowVersionSelector> workflowVersionSelectorMap =
                LogUsedTimeUtils.logUsedTime(log, "        fetch workflow selectors") {
                    fastqcArtefactService.fetchWorkflowVersionSelectorForSeqTracks(seqTracks, [workflowWes, workflowBash]).collectEntries {
                        assert it.project.state != Project.State.ARCHIVED
                        assert it.project.state != Project.State.DELETED
                        [(it.project): it]
                    }
                }

        Map<SeqTrack, List<FastqcArtefactDataWithFastqcProcessedFile>> groupedAdditionalDataPerSeqtrack =
                LogUsedTimeUtils.logUsedTime(log, "        group additional Artefacts") {
                    fastqcProcessedFileData.groupBy {
                        it.seqTrack
                    }
                }

        Map<Project, List<FastqcArtefactDataWithSeqTrack>> groupSeqTrackPerProject =
                LogUsedTimeUtils.logUsedTime(log, "        grouped per project") {
                    seqTrackData.groupBy {
                        it.project
                    }
                }

        LogUsedTimeUtils.logUsedTimeStartEnd(log, "        handle ${groupSeqTrackPerProject.size()} projects") {
            groupSeqTrackPerProject.each { Project project, List<FastqcArtefactDataWithSeqTrack> groups ->
                LogUsedTimeUtils.logUsedTimeStartEnd(log, "          handle project ${project} with ${groups.size()} groups") {
                    WorkflowVersionSelector matchingWorkflows = workflowVersionSelectorMap[project]
                    if (!matchingWorkflows) {
                        log.debug("            skip, since no workflow version is configured")
                        deciderResult.warnings << "Fastqc: Ignore ${project}, since no workflow version is configured".toString()
                        return
                    }
                    deciderResult.infos << "Fastqc: Use ${matchingWorkflows.workflowVersion} for ${project}".toString()

                    groups.each { FastqcArtefactDataWithSeqTrack fastqcArtefactData ->
                        List<FastqcArtefactDataWithFastqcProcessedFile> additionalArtefacts = groupedAdditionalDataPerSeqtrack[fastqcArtefactData.artefact]
                        deciderResult.add(createWorkflowRunsAndOutputArtefacts(
                                fastqcArtefactData,
                                additionalArtefacts,
                                rawSequenceFilesMap,
                                matchingWorkflows,
                                deciderAction)
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

    @Override
    List<DeciderCreateWorkflowAction> getSupportedActions() {
        return [DeciderCreateWorkflowAction.CREATE_MISSING, DeciderCreateWorkflowAction.SKIP]
    }

    protected DeciderResult createWorkflowRunsAndOutputArtefacts(FastqcArtefactDataWithSeqTrack fastqcArtefactData,
                                                                 List<FastqcArtefactDataWithFastqcProcessedFile> additionalArtefacts,
                                                                 Map<SeqTrack, List<RawSequenceFile>> rawSequenceFilesMap,
                                                                 WorkflowVersionSelector matchingWorkflow,
                                                                 Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = [:]) {
        DeciderResult deciderResult = new DeciderResult()
        SeqTrack seqTrack = fastqcArtefactData.artefact
        String seqTrackString = seqTrack.toString().replaceAll('<br>', ', ')
        deciderResult.infos << "process seqTrack ${seqTrackString}".toString()

        List<RawSequenceFile> rawSequenceFiles = rawSequenceFilesMap[seqTrack]

        if (additionalArtefacts && additionalArtefacts.size() == rawSequenceFiles.size()) {
            DeciderCreateWorkflowAction action = deciderAction[getClass()]
            switch (action) {
                case DeciderCreateWorkflowAction.CREATE_ALWAYS:
                    throw new DeciderCreateWorkflowActionException("The action CREATE_ALWAYS is not supported for fastqc")
                case DeciderCreateWorkflowAction.CREATE_MISSING_AND_NEWER:
                    throw new DeciderCreateWorkflowActionException("The action CREATE_MISSING_AND_NEWER is not supported for fastqc")
                default: // case DeciderCreateWorkflowActions.CREATE_MISSING: //(default)
                    if (additionalArtefacts.every { it.workflowArtefact?.state != WorkflowArtefact.State.FAILED }) {
                        deciderResult.warnings << "skip ${seqTrackString}, since fastqc already exist".toString()
                        return deciderResult
                    }
            }
        }

        Map<RawSequenceFile, FastqcProcessedFile> fastqcPerRawSequenceFile = additionalArtefacts ? additionalArtefacts.collectEntries {
            [(it.rawSequenceFile): it.artefact]
        } : [:]

        WorkflowVersion workflowVersion = matchingWorkflow.workflowVersion
        Workflow workflow = workflowVersion.workflow

        Map<RawSequenceFile, FastqcProcessedFile> fastqcProcessedFiles = rawSequenceFiles.collectEntries {
            FastqcProcessedFile fastqcProcessedFile = fastqcPerRawSequenceFile[it] ?: new FastqcProcessedFile([
                    sequenceFile     : it,
                    workDirectoryName: "", //will be set in workflow, since validation requires query, which slow down the creation
            ]).save(flush: false, deepValidate: false)
            [(it): fastqcProcessedFile]
        }

        List<String> displayName = generateWorkflowRunDisplayName(fastqcArtefactData)
        String shortName = "${workflow}: ${fastqcArtefactData.individual.pid} " +
                "${fastqcArtefactData.sampleType.displayName} ${fastqcArtefactData.seqType.displayNameWithLibraryLayout}"
        WorkflowRun run = workflowRunService.buildWorkflowRun(
                workflow,
                fastqcArtefactData.project.processingPriority,
                null,
                fastqcArtefactData.project,
                displayName,
                shortName,
                workflowVersion,
        )

        new WorkflowRunInputArtefact(
                workflowRun: run,
                role: BashFastQcWorkflow.INPUT_FASTQ,
                workflowArtefact: fastqcArtefactData.workflowArtefact,
        ).save(flush: false, deepValidate: false)

        List<WorkflowArtefact> result = []

        rawSequenceFiles.eachWithIndex { RawSequenceFile it, int i ->
            WorkflowArtefact workflowArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                    run,
                    "${BashFastQcWorkflow.OUTPUT_FASTQC}_${i + 1}",
                    ArtefactType.FASTQC,
                    displayName,
            )).save(flush: false, deepValidate: false)
            FastqcProcessedFile fastqcProcessedFile = fastqcProcessedFiles[it]
            fastqcProcessedFile.workflowArtefact = workflowArtefact
            fastqcProcessedFile.save(flush: false, deepValidate: false)
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
    private List<String> generateWorkflowRunDisplayName(FastqcArtefactDataWithSeqTrack fastqcArtefactData) {
        List<String> runDisplayName = [
                "project: ${fastqcArtefactData.project.name}",
                "individual: ${fastqcArtefactData.individual.displayName}",
                "sampleType: ${fastqcArtefactData.sampleType.displayName}",
                "seqType: ${fastqcArtefactData.seqType.displayNameWithLibraryLayout}",
                "run: ${fastqcArtefactData.run.name}",
                "lane: ${fastqcArtefactData.artefact.laneId}",
        ]*.toString()
        return runDisplayName
    }

    final String getWorkflowName() {
        return BashFastQcWorkflow.WORKFLOW
    }
}
