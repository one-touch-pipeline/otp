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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.*

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
    RoddyBamFileService roddyBamFileService

    @Autowired
    UnalignableSeqTrackEmailCreator unalignableSeqTrackEmailCreator

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WorkflowService workflowService

    abstract boolean requiresFastqcResults()

    abstract String getWorkflowName()

    abstract String getInputFastqRole()

    abstract String getInputFastqcRole()

    abstract String getOutputBamRole()

    abstract Pipeline.Name getPipelineName()

    abstract RoddyBamFile createBamFileWithoutFlush(Map properties)

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

        List<AlignmentArtefactData<RoddyBamFile>> dataBamFiles =
                alignmentArtefactService.fetchRelatedBamFileArtefactsForSeqTracks(seqTracks)
        dataBamFiles.removeAll(inputArtefactDataList.bamData)

        return new AlignmentArtefactDataList(dataSeqTracks, dataFastqcs, dataBamFiles)
    }

    @Override
    protected AlignmentAdditionalData fetchAdditionalData(AlignmentArtefactDataList inputArtefactDataList, Workflow workflow) {
        Collection<SeqTrack> seqTracks = inputArtefactDataList.seqTrackData*.artefact
        if (!seqTracks) {
            return new AlignmentAdditionalData([:], [:], [:], [:], [:], [:], null)
        }
        return new AlignmentAdditionalData(
                alignmentArtefactService.fetchReferenceGenome(workflow, seqTracks),
                alignmentArtefactService.fetchMergingCriteria((seqTracks)),
                alignmentArtefactService.fetchSpecificSeqPlatformGroup((seqTracks)),
                alignmentArtefactService.fetchDefaultSeqPlatformGroup(),
                alignmentArtefactService.fetchMergingWorkPackage(seqTracks),
                requiresFastqcResults() ? alignmentArtefactService.fetchRawSequenceFiles(seqTracks) : [:],
                pipelineService.findByPipelineName(pipelineName),
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
    protected DeciderResult createWorkflowRunsAndOutputArtefacts(ProjectSeqTypeGroup projectSeqTypeGroup, AlignmentDeciderGroup group,
                                                                 AlignmentArtefactDataList givenArtefacts, AlignmentArtefactDataList additionalArtefacts,
                                                                 AlignmentAdditionalData additionalData, WorkflowVersion version) {
        DeciderResult deciderResult = new DeciderResult()
        deciderResult.infos << "process group ${group}".toString()

        AlignmentArtefactDataList allArtefacts = new AlignmentArtefactDataList(
                givenArtefacts.seqTrackData + additionalArtefacts.seqTrackData,
                givenArtefacts.fastqcProcessedFileData + additionalArtefacts.fastqcProcessedFileData,
                givenArtefacts.bamData + additionalArtefacts.bamData,
        )

        RoddyBamFile existingBamFile = allArtefacts.bamData.find()?.artefact
        List<SeqTrack> seqTracks = allArtefacts.seqTrackData*.artefact

        if (seqTracks.empty) {
            deciderResult.warnings << "skip ${group}, since no seqTracks found".toString()
            return deciderResult
        }

        if (seqTracks as Set == existingBamFile?.seqTracks) {
            deciderResult.warnings << "skip ${group}, since existing BAM file with the same seqTracks found".toString()
            return deciderResult
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
        MergingWorkPackage workPackage = additionalData.mergingWorkPackageMap[alignmentWorkPackageGroup]

        if (workPackage) {
            if (workPackage.referenceGenome != referenceGenome) {
                deciderResult.warnings << ("skip ${group}, since existing MergingWorkPackage uses ReferenceGenome '${workPackage.referenceGenome}', " +
                        "but the configured one is '${referenceGenome}'").toString()
                return deciderResult
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
                String nonMatchingString = nonMatchingProperties.collect { String key, Entity value ->
                    [
                            key,
                            "- workPackage: ${workPackage[key]}",
                            "- seqTrack:    ${value}",
                    ].join('\n')
                }.join('\n')

                deciderResult.warnings << "skip ${group}, since existing MergingWorkPackage and Lanes do not match\n${nonMatchingString}".toString()
                UnalignableSeqTrackEmailCreator.MailContent content = unalignableSeqTrackEmailCreator.getMailContent(workPackage, seqTrack)
                mailHelperService.sendEmailToTicketSystem(content.subject, content.body)
                return deciderResult
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
        }
        workPackage.seqTracks = seqTracks as Set
        workPackage.save(flush: false, deepValidate: false)

        List<String> runDisplayName = [
                "project: ${projectSeqTypeGroup.project.name}",
                "individual: ${group.individual.displayName}",
                "sampleType: ${group.sampleType.displayName}",
                "seqType: ${group.seqType.displayNameWithLibraryLayout}",
        ]*.toString()
        List<String> artefactDisplayName = runDisplayName[1, -1]
        artefactDisplayName.remove(0)
        String shortName = "${workflowName}: ${group.individual.pid} ${group.sampleType.displayName} ${group.seqType.displayNameWithLibraryLayout}"

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                version.workflow,
                projectSeqTypeGroup.project.processingPriority,
                "", // set later
                projectSeqTypeGroup.project,
                runDisplayName,
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
                artefactDisplayName,
        ))

        int identifier = RoddyBamFile.nextIdentifier(workPackage)
        RoddyBamFile bamFile = createBamFileWithoutFlush([
                workflowArtefact: workflowOutputArtefact,
                workPackage: workPackage,
                identifier: identifier,
                workDirectoryName: "${RoddyBamFileService.WORK_DIR_PREFIX}_${identifier}",
                seqTracks: seqTrackSet,
                numberOfMergedLanes: seqTrackSet.size(),
        ])

        run.workDirectory = roddyBamFileService.getWorkDirectory(bamFile)
        run.save(flush: true, deepValidate: false)

        deciderResult.infos << "--> create bam file ${bamFile}".toString()
        deciderResult.newArtefacts << workflowOutputArtefact
        return deciderResult
    }
}
