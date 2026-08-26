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

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentWorkFileServiceFactoryService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.*

@SuppressWarnings('AbcMetric')
@Transactional
@Slf4j
abstract class AbstractAlignmentDecider extends AbstractWorkflowDecider<AlignmentArtefactDataList, AlignmentDeciderGroup, AlignmentAdditionalData> {

    @Autowired
    AlignmentArtefactService alignmentArtefactService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    PipelineService pipelineService

    @Autowired
    AlignmentWorkFileServiceFactoryService alignmentWorkFileServiceFactoryService

    @Autowired
    UnalignableSeqTrackEmailCreator unalignableSeqTrackEmailCreator

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WorkflowStateChangeService workflowStateChangeService

    @Autowired
    CommentService commentService

    abstract boolean requiresFastqcResults()

    abstract boolean allowsUnalignedCram()

    abstract String getWorkflowName()

    abstract String getInputFastqRole()

    abstract String getInputFastqcRole()

    abstract String getOutputBamRole()

    abstract Pipeline.Name getPipelineName()

    abstract AbstractBamFile createBamFileWithoutFlush(Map properties)

    @Override
    final protected Workflow getWorkflow() {
        return workflowService.getExactlyOneWorkflow(workflowName)
    }

    @Override
    final protected Set<ArtefactType> getSupportedInputArtefactTypes() {
        Set<ArtefactType> types = [ArtefactType.FASTQ] as Set
        if (requiresFastqcResults()) {
            types.add(ArtefactType.FASTQC)
        }
        if (allowsUnalignedCram()) {
            types.add(ArtefactType.UNALIGNED_CRAM)
        }
        return types
    }

    @Override
    protected AlignmentArtefactDataList fetchInputArtefacts(Collection<WorkflowArtefact> inputArtefacts, Set<SeqType> seqTypes) {
        return new AlignmentArtefactDataList(
                alignmentArtefactService.fetchSeqTrackArtefacts(inputArtefacts, seqTypes),
                requiresFastqcResults() ? alignmentArtefactService.fetchFastqcProcessedFileArtefacts(inputArtefacts, seqTypes) : [],
                []
        )
    }

    @Override
    protected AlignmentArtefactDataList fetchAdditionalArtefacts(AlignmentArtefactDataList inputArtefactDataList) {
        List<SeqTrack> seqTracks = inputArtefactDataList.seqTrackData*.artefact
        List<AlignmentArtefactData<SeqTrack>> dataSeqTracks =
                alignmentArtefactService.fetchRelatedSeqTrackArtefactsForSeqTracks(seqTracks)
        dataSeqTracks.removeAll(inputArtefactDataList.seqTrackData)

        List<AlignmentArtefactData<FastqcProcessedFile>> dataFastqcs = requiresFastqcResults() ?
                alignmentArtefactService.fetchRelatedFastqcArtefactsForSeqTracks(seqTracks) : []
        dataFastqcs.removeAll(inputArtefactDataList.fastqcProcessedFileData)

        List<AlignmentArtefactData<AbstractBamFile>> dataBamFiles =
                alignmentArtefactService.fetchRelatedBamFileArtefactsForSeqTracks(seqTracks)
        dataBamFiles.removeAll(inputArtefactDataList.bamData)

        return new AlignmentArtefactDataList(dataSeqTracks, dataFastqcs, dataBamFiles)
    }

    @Override
    protected AlignmentAdditionalData fetchAdditionalData(AlignmentArtefactDataList inputArtefactDataList,
                                                          AlignmentArtefactDataList additionalArtefactDataList, Workflow workflow) {
        Collection<SeqTrack> seqTracks = inputArtefactDataList.seqTrackData*.artefact
        if (!seqTracks) {
            return new AlignmentAdditionalData([:], [:], [:], [:], [:], [:], null, [:])
        }
        return new AlignmentAdditionalData(
                alignmentArtefactService.fetchReferenceGenome(workflow, seqTracks),
                alignmentArtefactService.fetchMergingCriteria((seqTracks)),
                alignmentArtefactService.fetchSpecificSeqPlatformGroup((seqTracks)),
                alignmentArtefactService.fetchDefaultSeqPlatformGroup(),
                alignmentArtefactService.fetchMergingWorkPackages(seqTracks),
                requiresFastqcResults() ? alignmentArtefactService.fetchRawSequenceFiles(seqTracks) : [:],
                pipelineService.findByPipelineName(pipelineName),
                alignmentArtefactService.fetchActiveAlignmentRunsPerWorkPackage(seqTracks),
        )
    }

    @Override
    protected List<WorkflowVersionSelector> fetchWorkflowVersionSelector(AlignmentArtefactDataList inputArtefactDataList, Workflow workflow) {
        Collection<SeqTrack> seqTracks = inputArtefactDataList.seqTrackData*.artefact
        return alignmentArtefactService.fetchWorkflowVersionSelectorForSeqTracks(workflow, seqTracks)
    }

    @Override
    protected Map<AlignmentDeciderGroup, AlignmentArtefactDataList> groupData(AlignmentArtefactDataList inputArtefactDataList,
                                                                              AlignmentAdditionalData additionalData,
                                                                              Map<String, String> userParams) {

        boolean ignoreSeqPlatformGroup = "TRUE".equalsIgnoreCase(userParams['ignoreSeqPlatformGroup']?.toString())

        Map<AlignmentDeciderGroup, AlignmentArtefactDataList> map = [:].withDefault {
            new AlignmentArtefactDataList([], [], [])
        }
        inputArtefactDataList.seqTrackData.each {
            map[createAlignmentDeciderGroup(it, ignoreSeqPlatformGroup, additionalData)].seqTrackData << it
        }
        inputArtefactDataList.fastqcProcessedFileData.each {
            map[createAlignmentDeciderGroup(it, ignoreSeqPlatformGroup, additionalData)].fastqcProcessedFileData << it
        }
        inputArtefactDataList.bamData.each {
            map[createAlignmentDeciderGroup(it, ignoreSeqPlatformGroup, additionalData, true)].bamData << it
        }
        return map
    }

    protected AlignmentDeciderGroup createAlignmentDeciderGroup(AlignmentArtefactData<?> data,
                                                                boolean ignoreSeqPlatformGroup,
                                                                AlignmentAdditionalData additionalData,
                                                                boolean fromBam = false) {
        ProjectSeqTypeGroup projectSeqTypePair = new ProjectSeqTypeGroup(data.project, data.seqType)
        MergingCriteria mergingCriteria = additionalData.mergingCriteriaMap[projectSeqTypePair]
        assert mergingCriteria

        LibraryPreparationKit libraryPreparationKit = mergingCriteria.useLibPrepKit ? data.libraryPreparationKit : null
        SeqPlatformGroup seqPlatformGroup
        if (fromBam) {
            seqPlatformGroup = ignoreSeqPlatformGroup ? null : data.seqPlatformGroup
        } else if (ignoreSeqPlatformGroup || mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING) {
            seqPlatformGroup = null
        } else {
            Map<SeqPlatform, SeqPlatformGroup> seqPlatformGroupMap =
                    mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC ?
                            additionalData.specificSeqPlatformGroupMap[projectSeqTypePair] :
                            mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT ?
                                    additionalData.defaultSeqPlatformGroupMap :
                                    [:]

            assert seqPlatformGroupMap: "No seqPlatformGroup defined for '${data.seqPlatform}'"
            seqPlatformGroup = seqPlatformGroupMap[data.seqPlatform]
            assert seqPlatformGroup: "No seqPlatformGroup defined for '${data.seqPlatform}'"
        }

        return new AlignmentDeciderGroup(data.individual, data.seqType, data.sampleType, data.sample, data.antibodyTarget, libraryPreparationKit,
                seqPlatformGroup)
    }

    @Override
    @SuppressWarnings(['CyclomaticComplexity', 'MethodSize'])
    protected DeciderResult createWorkflowRunsAndOutputArtefacts(ProjectSeqTypeGroup projectSeqTypeGroup, AlignmentDeciderGroup group,
                                                                 AlignmentArtefactDataList givenArtefacts, AlignmentArtefactDataList additionalArtefacts,
                                                                 AlignmentAdditionalData additionalData, WorkflowVersion version,
                                                                 Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = [:]) {
        DeciderResult deciderResult = new DeciderResult()
        deciderResult.infos << "process group ${group}".toString()

        AlignmentArtefactDataList allArtefacts = new AlignmentArtefactDataList(
                givenArtefacts.seqTrackData + additionalArtefacts.seqTrackData,
                givenArtefacts.fastqcProcessedFileData + additionalArtefacts.fastqcProcessedFileData,
                givenArtefacts.bamData + additionalArtefacts.bamData,
        )

        AlignmentArtefactData<AbstractBamFile> existingBamFileData = allArtefacts.bamData.find() as AlignmentArtefactData<AbstractBamFile>
        AbstractBamFile existingBamFile = existingBamFileData?.artefact
        List<SeqTrack> seqTracks = allArtefacts.seqTrackData*.artefact

        if (seqTracks.empty) {
            deciderResult.warnings << "skip ${group}, since no seqTracks found".toString()
            return deciderResult
        }
        if (seqTracks as Set == existingBamFile?.seqTracks) {
            DeciderCreateWorkflowAction action = deciderAction[getClass()]
            switch (action) {
                case DeciderCreateWorkflowAction.CREATE_ALWAYS:
                    deciderResult.warnings << "recreate ${group}, since action is CREATE_ALWAYS".toString()
                    break
                case DeciderCreateWorkflowAction.CREATE_MISSING_AND_NEWER:
                    if (existingBamFileData.version == version.workflowVersion) {
                        deciderResult.warnings << ("skip ${group}, since existing BAM file with the same seqTracks and version found, " +
                                "and action is CREATE_MISSING_AND_NEWER").toString()
                        return deciderResult
                    }
                    deciderResult.warnings << ("recreate ${group}, since existing BAM file with the same seqTracks has other version, " +
                            "and action is CREATE_MISSING_AND_NEWER").toString()
                    break
                default: // case DeciderCreateWorkflowAction.CREATE_MISSING: (default)
                    deciderResult.warnings << "skip ${group}, since existing BAM file with the same seqTracks found and action is CREATE_MISSING".toString()
                    return deciderResult
            }
        }

        Set<SeqTrack> seqTrackSet = seqTracks as Set

        if (requiresFastqcResults()) {
            List<FastqcProcessedFile> fastqcProcessedFiles = allArtefacts.fastqcProcessedFileData*.artefact
            List<SeqTrack> seqTracksWithMissingFastqc = seqTracks.findAll { SeqTrack seqTrack ->
                !additionalData.rawSequenceFileMap[seqTrack].every { RawSequenceFile rawSequenceFile ->
                    fastqcProcessedFiles.find { it.sequenceFile == rawSequenceFile }
                }
            }
            if (seqTracksWithMissingFastqc) {
                List<String> msg = []
                msg << "skip ${group}, since input contains the following fastq without all corresponding fastqc\n${seqTracksWithMissingFastqc}".toString()
                deciderResult.warnings << msg.join('\n')
                return deciderResult
            }
            List<FastqcProcessedFile> fastqcWithOutSeqTrack = fastqcProcessedFiles.findAll {
                !seqTrackSet.contains(it.sequenceFile.seqTrack)
            }
            if (fastqcWithOutSeqTrack) {
                deciderResult.warnings <<
                        "skip ${group}, since input contains the following fastqc without corresponding fastq\n${fastqcWithOutSeqTrack}".toString()
                return deciderResult
            }
        }

        if (!group.individual.species) {
            deciderResult.warnings << "skip ${group}, since no species is defined for individual ${group.individual.pid}".toString()
            return deciderResult
        }
        Set<SpeciesWithStrain> allSpecies = [group.individual.species] as Set
        if (group.sample.mixedInSpecies) {
            allSpecies.addAll(group.sample.mixedInSpecies)
        }

        ReferenceGenome referenceGenome = additionalData.referenceGenomeMap[projectSeqTypeGroup].get(allSpecies)
        if (!referenceGenome) {
            deciderResult.warnings <<
                    "skip ${group}, since no reference genome is configured for ${projectSeqTypeGroup} and species: '${allSpecies}'".toString()
            return deciderResult
        }

        AlignmentWorkPackageGroup alignmentWorkPackageGroup = new AlignmentWorkPackageGroup(group.sample, group.seqType, group.antibodyTarget)

        MergingWorkPackage workPackage
        try {
            workPackage = findOrCreateMergingWorkPackage(additionalData, alignmentWorkPackageGroup, seqTrackSet, group, referenceGenome, version)
        } catch (DeciderReferenceGenomeValidationException e) {
            deciderResult.warnings << "skip ${group}, since ${e.message}".toString()
            return deciderResult
        } catch (DeciderMergingWorkPackageValidationException e) {
            deciderResult.warnings << "skip ${group}, since ${e.message}".toString()
            if (e.sendsUnalignableSeqTrackEmail) {
                // Send email for unaligned SeqTrack issues
                if (e.workPackage) {
                    UnalignableSeqTrackEmailCreator.MailContent content = unalignableSeqTrackEmailCreator.getMailContent(e.workPackage, seqTracks.first())
                    mailHelperService.saveMail(content.subject, content.body)
                }
            }
            return deciderResult
        }

        workPackage.seqTracks = seqTracks as Set
        workPackage.save(flush: false, deepValidate: false)

        cancelConflictingRuns(workPackage, additionalData, deciderResult)

        List<String> displayName = [
                "project: ${projectSeqTypeGroup.project.name}",
                "individual: ${group.individual.displayName}",
                "sampleType: ${group.sampleType.displayName}",
                "seqType: ${group.seqType.displayNameWithLibraryLayout}",
        ]*.toString()
        String shortName = "${workflowName}: ${group.individual.pid} ${group.sampleType.displayName} ${group.seqType.displayNameWithLibraryLayout}"

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                version.workflow,
                projectSeqTypeGroup.project.processingPriority,
                "", // set later
                projectSeqTypeGroup.project,
                displayName,
                shortName,
                version,
        )

        allArtefacts.seqTrackData.findAll {
            seqTrackSet.contains(it.artefact)
        }.each {
            new WorkflowRunInputArtefact(
                    workflowRun: run,
                    role: "${inputFastqRole}_${it.artefact.id}",
                    workflowArtefact: it.workflowArtefact,
            ).save(flush: false, deepValidate: false)
        }
        if (requiresFastqcResults()) {
            allArtefacts.fastqcProcessedFileData.findAll {
                seqTrackSet.contains(it.artefact.sequenceFile.seqTrack)
            }.each {
                new WorkflowRunInputArtefact(
                        workflowRun: run,
                        role: "${inputFastqcRole}_${it.artefact.id}",
                        workflowArtefact: it.workflowArtefact,
                ).save(flush: false, deepValidate: false)
            }
        }

        WorkflowArtefact workflowOutputArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                run,
                outputBamRole,
                ArtefactType.BAM,
                displayName,
        ))

        AbstractBamFile bamFile = createBamFileWithoutFlush([
                workflowArtefact   : workflowOutputArtefact,
                workPackage        : workPackage,
                seqTracks          : seqTrackSet,
                numberOfMergedLanes: seqTrackSet.size(),
        ])

        run.workDirectory = alignmentWorkFileServiceFactoryService.getService(bamFile).getDirectoryPath(bamFile)
        run.save(flush: true, deepValidate: false)

        deciderResult.infos << "--> create bam file ${bamFile}".toString()
        deciderResult.newArtefacts << workflowOutputArtefact
        return deciderResult
    }

    /**
     * Extension point method to allow subclasses to customize MergingWorkPackage creation.
     * Standard workflows use the default implementation, while specialized workflows like
     * CellRanger can override this to create specific subclasses (e.g., CellRangerMergingWorkPackage).
     *
     * @param additionalData Additional data that may contain workflow-specific information needed for validation and creation of the MergingWorkPackage
     * @param alignmentWorkPackageGroup The group for which the MergingWorkPackage is being found or created
     * @param seqTracks The set of SeqTracks that are being processed for this group, used for validation against existing MergingWorkPackage properties
     * @param group The AlignmentDeciderGroup containing the context of the current processing group, used for validation and error reporting
     * @param referenceGenome The ReferenceGenome that should be used for the MergingWorkPackage, used for validation against MergingWorkPackage properties
     * @return The existing MergingWorkPackage if found and valid, or a new MergingWorkPackage if not found. Throws exceptions if validation fails
     */
    @SuppressWarnings(['UnusedMethodParameter', 'ParameterCount'])
    protected MergingWorkPackage findOrCreateMergingWorkPackage(AlignmentAdditionalData additionalData,
                                                                AlignmentWorkPackageGroup alignmentWorkPackageGroup,
                                                                Set<SeqTrack> seqTracks,
                                                                AlignmentDeciderGroup group,
                                                                ReferenceGenome referenceGenome,
                                                                WorkflowVersion version) {
        Set<MergingWorkPackage> workPackages = additionalData.mergingWorkPackageMap[alignmentWorkPackageGroup]
        // The constrain of MergingWorkPackage allows only one element at most to be saved for the same alignmentWorkPackageGroup
        // so it is safe to use atMostOneElement here
        MergingWorkPackage workPackage = workPackages ? CollectionUtils.atMostOneElement(workPackages) : null
        if (workPackage) {
            if (workPackage.referenceGenome != referenceGenome) {
                throw new DeciderReferenceGenomeValidationException(workPackage.referenceGenome, referenceGenome, group)
            }
            SeqTrack seqTrack = seqTracks.first()
            Map<String, Entity> properties = MergingWorkPackage.getMergingProperties(seqTrack)
            if (!group.seqPlatformGroup) {
                properties.remove('seqPlatformGroup') //since seqPlatformGroup may be ignored, it should not part of the check
            }
            Map<String, Entity> nonMatchingProperties = properties.findAll { String key, Entity value ->
                value != workPackage[key]
            }
            if (nonMatchingProperties) {
                throw new DeciderMergingWorkPackageValidationException(nonMatchingProperties, group, workPackage, true)
            }
        } else {
            workPackage = new MergingWorkPackage([
                    sample               : group.sample,
                    seqType              : group.seqType,
                    seqPlatformGroup     : group.seqPlatformGroup,
                    antibodyTarget       : group.antibodyTarget,
                    libraryPreparationKit: group.libraryPreparationKit,
                    referenceGenome      : referenceGenome,
                    pipeline             : additionalData.pipeline,
            ])
            workPackage.save(flush: false, deepValidate: false)
        }

        return workPackage
    }

    /**
     * Supersedes any still active or planned workflow runs for the same MergingWorkPackage, so that at most one
     * alignment run is active per MergingWorkPackage. This prevents the race in which a finishing run deletes the
     * work folder of a concurrent run of the same sample (otp-3020).
     *
     * Each conflicting run is killed (stopping its running cluster/WES jobs) and then set to the final failed state,
     * which withdraws its output artefact so it is neither re-detected as active nor restartable afterwards.
     *
     * The conflicting runs are taken from the prefetched {@link AlignmentAdditionalData#activeRunsPerWorkPackage}, so
     * that no query is executed inside the per-group processing loop.
     */
    private void cancelConflictingRuns(MergingWorkPackage workPackage, AlignmentAdditionalData additionalData, DeciderResult deciderResult) {
        List<WorkflowRun> activeRuns = additionalData.activeRunsPerWorkPackage?.get(workPackage) ?: []
        activeRuns.each { WorkflowRun activeRun ->
            if (activeRun.state in WorkflowRun.UNFINISHED_STATES) {
                try {
                    workflowService.killWorkflowRun(activeRun)
                } catch (FileAccessForProjectNotAllowedException e) {
                    log.warn("Could not kill superseded workflow run ${activeRun}, since its project is not accessible", e)
                }
            }

            workflowStateChangeService.changeStateToFinalFailed(activeRun)

            String cancelReason = "Superseded by newer workflow run"
            String commentText = activeRun.comment ? "${activeRun.comment.comment}\n${cancelReason}" : cancelReason
            commentService.saveCommentAsOtp(activeRun, commentText)

            deciderResult.infos << "Cancelled conflicting run ${activeRun} for MergingWorkPackage ${workPackage}".toString()
        }
    }
}
