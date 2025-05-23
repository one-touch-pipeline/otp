/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.test.hibernate.HibernateSpec

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.workflowExecution.decider.ProjectSeqTypeGroup
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentArtefactData
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentWorkPackageGroup

import java.time.LocalDate

class AlignmentArtefactServiceSpec extends HibernateSpec implements WorkflowSystemDomainFactory, RoddyPanCancerFactory, FastqcDomainFactory, AlignmentPipelineFactory {

    private AlignmentArtefactService alignmentArtefactService

    WorkflowArtefact workflowArtefactSeqTrack1
    WorkflowArtefact workflowArtefactSeqTrack2
    WorkflowArtefact workflowArtefactSeqTrackRelated
    WorkflowArtefact workflowArtefactFastqc1
    WorkflowArtefact workflowArtefactFastqc2
    WorkflowArtefact workflowArtefactFastqcRelated
    WorkflowArtefact workflowArtefactBam1
    WorkflowArtefact workflowArtefactBamNew
    WorkflowArtefact workflowArtefactBam2
    WorkflowArtefact workflowArtefactBamRelated
    WorkflowArtefact workflowArtefactBamRelatedNew
    WorkflowArtefact workflowArtefactSingleCellBam
    WorkflowArtefact workflowArtefactSingleCellBamRelated
    WorkflowArtefact workflowArtefactSingleCellBamNew
    WorkflowArtefact workflowArtefactSingleCellBamRelatedNew

    SeqTrack seqTrack1
    SeqTrack seqTrack2
    SeqTrack seqTrackRelated
    FastqcProcessedFile fastqc1
    FastqcProcessedFile fastqc2
    FastqcProcessedFile fastqcRelated
    RoddyBamFile bamFile1
    RoddyBamFile bamFile2
    RoddyBamFile bamFileNew
    RoddyBamFile bamFileRelated
    RoddyBamFile bamFileRelatedNew
    SingleCellBamFile singleCellBamFile1
    SingleCellBamFile singleCellBamFile1New
    SingleCellBamFile singleCellBamFileRelated
    SingleCellBamFile singleCellBamFileRelatedNew

    List<WorkflowArtefact> workflowArtefacts
    List<SeqType> seqTypes
    List<SeqTrack> seqTracks
    List<SeqTrack> seqTracksNew
    List<SeqTrack> seqTracksCellRanger

    @Override
    List<Class> getDomainClasses() {
        return [
                FastqFile,
                FastqImportInstance,
                FastqcProcessedFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                ReferenceGenomeSelector,
                RoddyBamFile,
                WorkflowArtefact,
                WorkflowVersionSelector,
                CellRangerMergingWorkPackage,
                SingleCellBamFile,
        ]
    }

    void setup() {
        alignmentArtefactService = new AlignmentArtefactService()
    }

    void setupData() {
        // artefact in input and seqType in input
        workflowArtefactSeqTrack1 = createWorkflowArtefact([artefactType: ArtefactType.FASTQ])
        seqTrack1 = createSeqTrackWithTwoFastqFileAndSpecies([workflowArtefact: workflowArtefactSeqTrack1])

        workflowArtefactFastqc1 = createWorkflowArtefact([artefactType: ArtefactType.FASTQC])
        fastqc1 = createFastqcProcessedFileWithSpecies([workflowArtefact: workflowArtefactFastqc1])

        workflowArtefactBam1 = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFile1 = createBamFileWithSpecies([workflowArtefact: workflowArtefactBam1])

        workflowArtefactBamNew = createWorkflowArtefact([artefactType: ArtefactType.BAM, producedBy: createWorkflowRun()])
        bamFileNew = createBamFileWithSpecies([workflowArtefact: workflowArtefactBamNew, config: null])

        workflowArtefactSingleCellBam = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        singleCellBamFile1 = CellRangerFactoryInstance.INSTANCE.createBamFile([workflowArtefact: workflowArtefactSingleCellBam])

        workflowArtefactSingleCellBamNew = createWorkflowArtefact([artefactType: ArtefactType.BAM, producedBy: createWorkflowRun()])
        singleCellBamFile1New = CellRangerFactoryInstance.INSTANCE.createBamFile([workflowArtefact: workflowArtefactSingleCellBamNew])

        // artefact in input, but seqType not in input
        workflowArtefactSeqTrack2 = createWorkflowArtefact([artefactType: ArtefactType.FASTQ])
        seqTrack2 = createSeqTrackWithTwoFastqFileAndSpecies([workflowArtefact: workflowArtefactSeqTrack2])

        workflowArtefactFastqc2 = createWorkflowArtefact([artefactType: ArtefactType.FASTQC])
        fastqc2 = createFastqcProcessedFileWithSpecies([workflowArtefact: workflowArtefactFastqc2])

        workflowArtefactBam2 = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFile2 = createBamFileWithSpecies([workflowArtefact: workflowArtefactBam2])

        // input list
        workflowArtefacts = [
                workflowArtefactSeqTrack1,
                workflowArtefactFastqc1,
                workflowArtefactBam1,
                workflowArtefactSeqTrack2,
                workflowArtefactFastqc2,
                workflowArtefactBam2,
                workflowArtefactSingleCellBam,
                workflowArtefactBamNew,
                workflowArtefactSingleCellBamNew,
        ]
        seqTypes = [
                seqTrack1.seqType,
                fastqc1.sequenceFile.seqType,
                bamFile1.seqType,
                bamFileNew.seqType,
                singleCellBamFile1.seqType,
        ]

        seqTracks = [
                seqTrack1,
                fastqc1.sequenceFile.seqTrack,
        ] + bamFile1.containedSeqTracks

        seqTracksNew = [
                seqTrack1,
                fastqc1.sequenceFile.seqTrack,
        ] + bamFileNew.containedSeqTracks

        seqTracksCellRanger = singleCellBamFile1.containedSeqTracks as List
    }

    void setupDataWithRelated() {
        setupData()

        // artefact related to input, but not part of the artefact input
        workflowArtefactSeqTrackRelated = createWorkflowArtefact([artefactType: ArtefactType.FASTQ])
        seqTrackRelated = createSeqTrackWithTwoFastqFile([
                workflowArtefact: workflowArtefactSeqTrackRelated,
                sample          : seqTrack1.sample,
                seqType         : seqTrack1.seqType,
        ])

        workflowArtefactFastqcRelated = createWorkflowArtefact([artefactType: ArtefactType.FASTQC])
        fastqcRelated = createFastqcProcessedFile([
                workflowArtefact: workflowArtefactFastqcRelated,
                sequenceFile    : createFastqFile([
                        seqTrack: createSeqTrack([
                                sample : fastqc1.sequenceFile.sample,
                                seqType: fastqc1.sequenceFile.seqType,
                        ]),
                ]),
        ])

        workflowArtefactBamRelated = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFileRelated = createBamFile([
                workflowArtefact: workflowArtefactBamRelated,
                workPackage     : bamFile1.workPackage,
        ])

        workflowArtefactBamRelatedNew = createWorkflowArtefact([artefactType: ArtefactType.BAM, producedBy: createWorkflowRun()])
        bamFileRelatedNew = createBamFile([
                workflowArtefact: workflowArtefactBamRelatedNew,
                workPackage     : bamFileNew.workPackage,
                config: null,
        ])

        workflowArtefactSingleCellBamRelated = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        singleCellBamFileRelated = CellRangerFactoryInstance.INSTANCE.createBamFile([
                workflowArtefact: workflowArtefactSingleCellBamRelated,
                workPackage     : singleCellBamFile1.workPackage,
        ])

        workflowArtefactSingleCellBamRelatedNew = createWorkflowArtefact([artefactType: ArtefactType.BAM, producedBy: createWorkflowRun()])
        singleCellBamFileRelatedNew = CellRangerFactoryInstance.INSTANCE.createBamFile([
                workflowArtefact: workflowArtefactSingleCellBamRelatedNew,
                workPackage     : singleCellBamFile1New.workPackage,
        ])
    }

    void "fetchSeqTrackArtefacts, when called for workflowArtefacts and seqTypes, then return AlignmentArtefactData of expected SeqTrack"() {
        given:
        setupData()

        AlignmentArtefactData<SeqTrack> expected = createAlignmentArtefactDataForSeqTrack(seqTrack1)

        when:
        List<AlignmentArtefactData<SeqTrack>> result = alignmentArtefactService.fetchSeqTrackArtefacts(workflowArtefacts, seqTypes)

        then:
        result.size() == 1
        result.first() == expected
        result.first().version == null
    }

    void "fetchFastqcProcessedFileArtefacts, when called for workflowArtefacts and seqTypes, then return AlignmentArtefactData of expected FastqcProcessedFile"() {
        given:
        setupData()

        AlignmentArtefactData<FastqcProcessedFile> expected = createAlignmentArtefactDataForFastqcProcessedFile(fastqc1)

        when:
        List<AlignmentArtefactData<FastqcProcessedFile>> result = alignmentArtefactService.fetchFastqcProcessedFileArtefacts(workflowArtefacts, seqTypes)

        then:
        result.size() == 1
        result.first() == expected
        result.first().version == null
    }

    void "fetchRelatedSeqTrackArtefactsForSeqTracks, when called for workflowArtefacts and seqTypes, then return AlignmentArtefactData of expected SeqTrack"() {
        given:
        setupDataWithRelated()

        List<AlignmentArtefactData<SeqTrack>> expected = [
                createAlignmentArtefactDataForSeqTrack(seqTrack1),
                createAlignmentArtefactDataForSeqTrack(seqTrackRelated),
        ]

        when:
        List<AlignmentArtefactData<SeqTrack>> result = alignmentArtefactService.fetchRelatedSeqTrackArtefactsForSeqTracks(seqTracks)

        then:
        result.size() == 2
        TestCase.assertContainSame(result, expected)
        result.first().version == null
    }

    void "fetchRelatedFastqcArtefactsForSeqTracks, when called for seqTracks, then return AlignmentArtefactData of expected FastqcProcessedFile"() {
        given:
        setupDataWithRelated()

        List<AlignmentArtefactData<FastqcProcessedFile>> expected = [
                createAlignmentArtefactDataForFastqcProcessedFile(fastqc1),
                createAlignmentArtefactDataForFastqcProcessedFile(fastqcRelated),
        ]

        when:
        List<AlignmentArtefactData<FastqcProcessedFile>> result = alignmentArtefactService.fetchRelatedFastqcArtefactsForSeqTracks(seqTracks)

        then:
        result.size() == 2
        TestCase.assertContainSame(result, expected)
        result.first().version == null
    }

    void "fetchRelatedBamFileArtefactsForSeqTracks, for roddy when called for seqTracks in old system, then return AlignmentArtefactData of expected BamFile"() {
        given:
        setupDataWithRelated()

        List<AlignmentArtefactData<RoddyBamFile>> expected = [
                createAlignmentArtefactDataForRoddyBamFile(bamFileRelated),
        ]

        when:
        List<AlignmentArtefactData<RoddyBamFile>> result = alignmentArtefactService.fetchRelatedBamFileArtefactsForSeqTracks(seqTracks)

        then:
        result.size() == 1
        TestCase.assertContainSame(result, expected)
        result.first().version == bamFileRelated.config.programVersion.split(':')[1]
    }

    void "fetchRelatedBamFileArtefactsForSeqTracks, for roddy when called for seqTracks in new system, then return AlignmentArtefactData of expected BamFile"() {
        given:
        setupDataWithRelated()
        List<AlignmentArtefactData<RoddyBamFile>> expected = [
                createAlignmentArtefactDataForRoddyBamFile(bamFileRelatedNew),
        ]

        when:
        List<AlignmentArtefactData<AbstractBamFile>> result = alignmentArtefactService.fetchRelatedBamFileArtefactsForSeqTracks(seqTracksNew)

        then:
        result.size() == 1
        TestCase.assertContainSame(result, expected)
        result.first().version == bamFileRelatedNew.workflowArtefact.producedBy.workflowVersion.workflowVersion
    }

    void "fetchRelatedBamFileArtefactsForSeqTracks, for cellRanger when called for seqTracks in old system, then return AlignmentArtefactData of expected BamFile"() {
        given:
        setupDataWithRelated()
        List<AlignmentArtefactData<SingleCellBamFile>> expected = [
                createAlignmentArtefactDataForSingleCellBamFile(singleCellBamFileRelated),
        ]

        when:
        List<AlignmentArtefactData<SingleCellBamFile>> result = alignmentArtefactService.fetchRelatedBamFileArtefactsForSeqTracks(seqTracksCellRanger)

        then:
        result.size() == 1
        TestCase.assertContainSame(result, expected)
        result.first().version == singleCellBamFileRelated.workPackage.config.programVersion
    }

    void "fetchRelatedBamFileArtefactsForSeqTracks, for cellRanger when called for seqTracks in new system, then return AlignmentArtefactData of expected BamFile"() {
        given:
        setupDataWithRelated()
        List<AlignmentArtefactData<SingleCellBamFile>> expected = [
                createAlignmentArtefactDataForSingleCellBamFile(singleCellBamFileRelatedNew),
        ]

        when:
        List<AlignmentArtefactData<SingleCellBamFile>> result = alignmentArtefactService.fetchRelatedBamFileArtefactsForSeqTracks(singleCellBamFile1New.containedSeqTracks)

        then:
        result.size() == 1
        TestCase.assertContainSame(result, expected)
        result.first().version == singleCellBamFileRelatedNew.workflowArtefact.producedBy.workflowVersion.workflowVersion
    }

    void "fetchWorkflowVersionSelectorForSeqTracks, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        WorkflowVersion workflowVersion = createWorkflowVersion()

        WorkflowVersionSelector selector = createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : seqTrack1.project,
                seqType        : seqTrack1.seqType,
        ])

        createWorkflowVersionSelector([
                project: seqTrack1.project,
                seqType: seqTrack1.seqType,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                seqType        : seqTrack1.seqType,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : seqTrack1.project,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : seqTrack1.project,
                seqType        : seqTrack1.seqType,
                deprecationDate: LocalDate.now(),
        ])

        when:
        List<WorkflowVersionSelector> result = alignmentArtefactService.fetchWorkflowVersionSelectorForSeqTracks(workflowVersion.workflow, seqTracks)

        then:
        result.size() == 1
        result.first() == selector
    }

    void "fetchReferenceGenome, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        Workflow workflow = createWorkflow()

        Set<SpeciesWithStrain> speciesWithStrain1 = [seqTrack1.individual.species] as Set
        ReferenceGenome referenceGenome1 = createReferenceGenome([
                speciesWithStrain: speciesWithStrain1,
                species          : [],
        ])
        ProjectSeqTypeGroup group1 = new ProjectSeqTypeGroup(seqTrack1.project, seqTrack1.seqType)

        Set<SpeciesWithStrain> speciesWithStrain2 = [fastqc1.sequenceFile.individual.species] as Set
        ReferenceGenome referenceGenome2 = createReferenceGenome([
                speciesWithStrain: speciesWithStrain2,
                species          : [],
        ])
        createReferenceGenomeSelector([
                referenceGenome: referenceGenome2,
                project        : fastqc1.sequenceFile.project,
                seqType        : fastqc1.sequenceFile.seqType,
                workflow       : workflow,
                species        : speciesWithStrain2,
        ])
        ProjectSeqTypeGroup group2 = new ProjectSeqTypeGroup(fastqc1.sequenceFile.project, fastqc1.sequenceFile.seqType)

        Set<SpeciesWithStrain> speciesWithStrain3 = [bamFile1.individual.species] as Set
        ReferenceGenome referenceGenome3 = createReferenceGenome([
                speciesWithStrain: speciesWithStrain3,
                species          : [],
        ])
        createReferenceGenomeSelector([
                referenceGenome: referenceGenome3,
                project        : bamFile1.project,
                seqType        : bamFile1.seqType,
                workflow       : workflow,
                species        : speciesWithStrain3,
        ])
        ProjectSeqTypeGroup group3 = new ProjectSeqTypeGroup(bamFile1.project, bamFile1.seqType)

        createReferenceGenomeSelector([
                project : seqTrack1.project,
                seqType : seqTrack1.seqType,
                workflow: workflow,
                species : speciesWithStrain1.clone(),
        ])
        createReferenceGenomeSelector([
                referenceGenome: referenceGenome1,
                seqType        : seqTrack1.seqType,
                workflow       : workflow,
                species        : speciesWithStrain1.clone(),
        ])
        createReferenceGenomeSelector([
                referenceGenome: referenceGenome1,
                project        : seqTrack1.project,
                workflow       : workflow,
                species        : speciesWithStrain1.clone(),
        ])
        createReferenceGenomeSelector([
                referenceGenome: referenceGenome1,
                project        : seqTrack1.project,
                seqType        : seqTrack1.seqType,
                species        : speciesWithStrain1.clone(),
        ])
        createReferenceGenomeSelector([
                referenceGenome: referenceGenome1,
                project        : seqTrack1.project,
                seqType        : seqTrack1.seqType,
                workflow       : workflow,
        ])

        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> expected = [
                (group1): [
                        (speciesWithStrain1): referenceGenome1,
                ],
                (group2): [
                        (speciesWithStrain2): referenceGenome2,
                ],
                (group3): [
                        (speciesWithStrain3): referenceGenome3,
                ],
        ]

        when:
        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> result = alignmentArtefactService.fetchReferenceGenome(workflow, seqTracks)

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "fetchMergingCriteria, when called for seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        MergingCriteria mergingCriteria1 = createMergingCriteria([
                project: seqTrack1.project,
                seqType: seqTrack1.seqType,
        ])
        ProjectSeqTypeGroup group1 = new ProjectSeqTypeGroup(seqTrack1.project, seqTrack1.seqType)

        MergingCriteria mergingCriteria2 = createMergingCriteria([
                project: fastqc1.sequenceFile.project,
                seqType: fastqc1.sequenceFile.seqType,
        ])
        ProjectSeqTypeGroup group2 = new ProjectSeqTypeGroup(fastqc1.sequenceFile.project, fastqc1.sequenceFile.seqType)

        MergingCriteria mergingCriteria3 = createMergingCriteriaLazy([
                project: bamFile1.project,
                seqType: bamFile1.seqType,
        ])
        ProjectSeqTypeGroup group3 = new ProjectSeqTypeGroup(bamFile1.project, bamFile1.seqType)

        createMergingCriteria([
                seqType: seqTrack1.seqType,
        ])
        createMergingCriteria([
                project: seqTrack1.project,
        ])

        Map<ProjectSeqTypeGroup, MergingCriteria> expected = [
                (group1): mergingCriteria1,
                (group2): mergingCriteria2,
                (group3): mergingCriteria3,
        ]

        when:
        Map<ProjectSeqTypeGroup, MergingCriteria> result = alignmentArtefactService.fetchMergingCriteria(seqTracks)

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "fetchSpecificSeqPlatformGroup, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        MergingCriteria mergingCriteria1 = createMergingCriteria([
                project            : seqTrack1.project,
                seqType            : seqTrack1.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])
        SeqPlatformGroup seqPlatformGroup1 = createSeqPlatformGroup([
                mergingCriteria: mergingCriteria1,
                seqPlatforms   : [seqTrack1.seqPlatform],
        ])
        ProjectSeqTypeGroup group1 = new ProjectSeqTypeGroup(seqTrack1.project, seqTrack1.seqType)

        MergingCriteria mergingCriteria2 = createMergingCriteria([
                project            : fastqc1.sequenceFile.project,
                seqType            : fastqc1.sequenceFile.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])
        SeqPlatformGroup seqPlatformGroup2 = createSeqPlatformGroup([
                mergingCriteria: mergingCriteria2,
                seqPlatforms   : [fastqc1.sequenceFile.seqTrack.seqPlatform],
        ])
        ProjectSeqTypeGroup group2 = new ProjectSeqTypeGroup(fastqc1.sequenceFile.project, fastqc1.sequenceFile.seqType)

        MergingCriteria mergingCriteria3 = createMergingCriteriaLazy([
                project: bamFile1.project,
                seqType: bamFile1.seqType,
        ])
        mergingCriteria3.useSeqPlatformGroup = MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        mergingCriteria3.save(flush: true)
        SeqPlatform seqPlatform3 = bamFile1.mergingWorkPackage.seqPlatformGroup.seqPlatforms.first()
        SeqPlatformGroup seqPlatformGroup3 = createSeqPlatformGroup([
                mergingCriteria: mergingCriteria3,
                seqPlatforms   : [seqPlatform3],
        ])
        ProjectSeqTypeGroup group3 = new ProjectSeqTypeGroup(bamFile1.project, bamFile1.seqType)

        createMergingCriteria([
                seqType            : seqTrack1.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])
        createMergingCriteria([
                project            : seqTrack1.project,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])

        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> expected = [
                (group1): [
                        (seqTrack1.seqPlatform): seqPlatformGroup1,
                ],
                (group2): [
                        (fastqc1.sequenceFile.seqTrack.seqPlatform): seqPlatformGroup2,
                ],
                (group3): [
                        (seqPlatform3): seqPlatformGroup3,
                ],
        ]

        when:
        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> result = alignmentArtefactService.fetchSpecificSeqPlatformGroup(seqTracks)

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "fetchDefaultSeqPlatformGroup, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:

        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()
        SeqPlatform seqPlatform3 = createSeqPlatform()

        SeqPlatformGroup seqPlatformGroup1 = createSeqPlatformGroup([seqPlatforms: [seqPlatform1]])
        SeqPlatformGroup seqPlatformGroup2 = createSeqPlatformGroup([seqPlatforms: [seqPlatform2]])
        SeqPlatformGroup seqPlatformGroup3 = createSeqPlatformGroup([seqPlatforms: [seqPlatform3]])

        createSeqPlatformGroup([
                seqPlatforms   : [seqPlatform1],
                mergingCriteria: createMergingCriteria([
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
                ]),
        ])
        createSeqPlatformGroup([
                seqPlatforms   : [seqPlatform2],
                mergingCriteria: createMergingCriteria([
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
                ]),
        ])
        createSeqPlatformGroup([
                seqPlatforms   : [seqPlatform3],
                mergingCriteria: createMergingCriteria([
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
                ]),
        ])

        Map<SeqPlatform, SeqPlatformGroup> expected = [
                (seqPlatform1): seqPlatformGroup1,
                (seqPlatform2): seqPlatformGroup2,
                (seqPlatform3): seqPlatformGroup3,
        ]

        when:
        Map<SeqPlatform, SeqPlatformGroup> result = alignmentArtefactService.fetchDefaultSeqPlatformGroup()

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "fetchMergingWorkPackage, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        AlignmentWorkPackageGroup group3 = new AlignmentWorkPackageGroup(
                bamFile1.mergingWorkPackage.sample, bamFile1.mergingWorkPackage.seqType, bamFile1.mergingWorkPackage.antibodyTarget)

        Map<AlignmentWorkPackageGroup, MergingWorkPackage> expected = [
                (group3): bamFile1.mergingWorkPackage,
        ]

        when:
        Map<AlignmentWorkPackageGroup, MergingWorkPackage> result = alignmentArtefactService.fetchMergingWorkPackage(seqTracks)

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "fetchRawSequenceFiles, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        Map<SeqTrack, List<RawSequenceFile>> expected = [
                (seqTrack1)                    : seqTrack1.sequenceFiles,
                (fastqc1.sequenceFile.seqTrack): [fastqc1.sequenceFile],
                (bamFile1.seqTracks.first())   : bamFile1.seqTracks.first().sequenceFiles,
        ]

        when:
        Map<SeqTrack, List<RawSequenceFile>> result = alignmentArtefactService.fetchRawSequenceFiles(seqTracks)

        then:
        TestCase.assertContainSame(result, expected)
    }

    private SeqTrack createSeqTrackWithTwoFastqFileAndSpecies(Map parameters) {
        return createSeqTrackWithTwoFastqFile([
                sample: createSample([
                        individual: createIndividual([
                                species: createSpeciesWithStrain(),
                        ]),
                ]),
        ] + parameters)
    }

    private FastqcProcessedFile createFastqcProcessedFileWithSpecies(Map parameters) {
        return createFastqcProcessedFile([
                sequenceFile: createFastqFile([
                        seqTrack: createSeqTrack([
                                sample: createSample([
                                        individual: createIndividual([
                                                species: createSpeciesWithStrain(),
                                        ]),
                                ]),
                        ]),
                ]),
        ] + parameters)
    }

    private RoddyBamFile createBamFileWithSpecies(Map parameters) {
        return createBamFile([
                workPackage: createMergingWorkPackage([
                        sample: createSample([
                                individual: createIndividual([
                                        species: createSpeciesWithStrain(),
                                ]),
                        ]),
                ]),
        ] + parameters)
    }

    private AlignmentArtefactData<SeqTrack> createAlignmentArtefactDataForSeqTrack(SeqTrack seqTrack) {
        return new AlignmentArtefactData<SeqTrack>(
                seqTrack.workflowArtefact,
                seqTrack,
                seqTrack.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion,
                seqTrack.project,
                seqTrack.seqType,
                seqTrack.individual,
                seqTrack.sampleType,
                seqTrack.sample,
                seqTrack.antibodyTarget,
                seqTrack.libraryPreparationKit,
                seqTrack.seqPlatform,
                null
        )
    }

    private AlignmentArtefactData<FastqcProcessedFile> createAlignmentArtefactDataForFastqcProcessedFile(FastqcProcessedFile fastqcProcessedFile) {
        SeqTrack seqTrack = fastqcProcessedFile.sequenceFile.seqTrack
        return new AlignmentArtefactData<FastqcProcessedFile>(
                fastqcProcessedFile.workflowArtefact,
                fastqcProcessedFile,
                fastqcProcessedFile.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion,
                seqTrack.project,
                seqTrack.seqType,
                seqTrack.individual,
                seqTrack.sampleType,
                seqTrack.sample,
                seqTrack.antibodyTarget,
                seqTrack.libraryPreparationKit,
                seqTrack.seqPlatform,
                null
        )
    }

    private AlignmentArtefactData<RoddyBamFile> createAlignmentArtefactDataForRoddyBamFile(RoddyBamFile bamFile) {
        MergingWorkPackage workPackage = bamFile.workPackage
        return new AlignmentArtefactData<RoddyBamFile>(
                bamFile.workflowArtefact,
                bamFile,
                bamFile.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion ?: bamFile.config?.programVersion,
                workPackage.project,
                workPackage.seqType,
                workPackage.individual,
                workPackage.sampleType,
                workPackage.sample,
                workPackage.antibodyTarget,
                workPackage.libraryPreparationKit,
                null,
                workPackage.seqPlatformGroup
        )
    }

    private AlignmentArtefactData<SingleCellBamFile> createAlignmentArtefactDataForSingleCellBamFile(SingleCellBamFile bamFile) {
        MergingWorkPackage workPackage = bamFile.workPackage
        return new AlignmentArtefactData<SingleCellBamFile>(
                bamFile.workflowArtefact,
                bamFile,
                bamFile.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion ?: bamFile.workPackage.config?.programVersion,
                workPackage.project,
                workPackage.seqType,
                workPackage.individual,
                workPackage.sampleType,
                workPackage.sample,
                workPackage.antibodyTarget,
                workPackage.libraryPreparationKit,
                null,
                workPackage.seqPlatformGroup
        )
    }
}
