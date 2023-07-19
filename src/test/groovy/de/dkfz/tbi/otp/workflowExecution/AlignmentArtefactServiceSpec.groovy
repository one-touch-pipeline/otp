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
package de.dkfz.tbi.otp.workflowExecution

import grails.test.hibernate.HibernateSpec

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.workflowExecution.decider.ProjectSeqTypeGroup
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentArtefactData
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentWorkPackageGroup

import java.time.LocalDate

class AlignmentArtefactServiceSpec extends HibernateSpec implements WorkflowSystemDomainFactory, RoddyPancanFactory, FastqcDomainFactory {

    private AlignmentArtefactService alignmentArtefactService

    WorkflowArtefact workflowArtefactSeqTrack1
    WorkflowArtefact workflowArtefactSeqTrack2
    WorkflowArtefact workflowArtefactSeqTrackRelated
    WorkflowArtefact workflowArtefactFastqc1
    WorkflowArtefact workflowArtefactFastqc2
    WorkflowArtefact workflowArtefactFastqcRelated
    WorkflowArtefact workflowArtefactBam1
    WorkflowArtefact workflowArtefactBam2
    WorkflowArtefact workflowArtefactBamRelated

    SeqTrack seqTrack1
    SeqTrack seqTrack2
    SeqTrack seqTrackRelated
    FastqcProcessedFile fastqc1
    FastqcProcessedFile fastqc2
    FastqcProcessedFile fastqcRelated
    RoddyBamFile bamFile1
    RoddyBamFile bamFile2
    RoddyBamFile bamFileRelated

    List<WorkflowArtefact> workflowArtefacts
    List<SeqType> seqTypes
    List<SeqTrack> seqTracks

    @Override
    List<Class> getDomainClasses() {
        return [
                FastqImportInstance,
                FastqcProcessedFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                ReferenceGenomeSelector,
                RoddyBamFile,
                WorkflowArtefact,
                WorkflowVersionSelector,
        ]
    }

    void setup() {
        alignmentArtefactService = new AlignmentArtefactService()
    }

    void setupData() {
        //artefact in input and seqType in input
        workflowArtefactSeqTrack1 = createWorkflowArtefact([artefactType: ArtefactType.FASTQ])
        seqTrack1 = createSeqTrackWithTwoDataFileAndSpecies([workflowArtefact: workflowArtefactSeqTrack1])
        workflowArtefactFastqc1 = createWorkflowArtefact([artefactType: ArtefactType.FASTQC])
        fastqc1 = createFastqcProcessedFileWithSpecies([workflowArtefact: workflowArtefactFastqc1])
        workflowArtefactBam1 = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFile1 = createBamFileWithSpecies([workflowArtefact: workflowArtefactBam1])

        //artefact in input, but seqType not in input
        workflowArtefactSeqTrack2 = createWorkflowArtefact([artefactType: ArtefactType.FASTQ])
        seqTrack2 = createSeqTrackWithTwoDataFileAndSpecies([workflowArtefact: workflowArtefactSeqTrack2])
        workflowArtefactFastqc2 = createWorkflowArtefact([artefactType: ArtefactType.FASTQC])
        fastqc2 = createFastqcProcessedFileWithSpecies([workflowArtefact: workflowArtefactFastqc2])
        workflowArtefactBam2 = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFile2 = createBamFileWithSpecies([workflowArtefact: workflowArtefactBam2])

        //input list
        workflowArtefacts = [
                workflowArtefactSeqTrack1,
                workflowArtefactFastqc1,
                workflowArtefactBam1,
                workflowArtefactSeqTrack2,
                workflowArtefactFastqc2,
                workflowArtefactBam2,
        ]
        seqTypes = [
                seqTrack1.seqType,
                fastqc1.dataFile.seqType,
                bamFile1.seqType,
        ]

        seqTracks = [
                seqTrack1,
                fastqc1.dataFile.seqTrack,
        ] + bamFile1.containedSeqTracks
    }

    void setupDataWithRelated() {
        setupData()

        //artefact related to input, but not part of the artefact input
        workflowArtefactSeqTrackRelated = createWorkflowArtefact([artefactType: ArtefactType.FASTQ])
        seqTrackRelated = createSeqTrackWithTwoDataFile([
                workflowArtefact: workflowArtefactSeqTrackRelated,
                sample          : seqTrack1.sample,
                seqType         : seqTrack1.seqType,
        ])
        workflowArtefactFastqcRelated = createWorkflowArtefact([artefactType: ArtefactType.FASTQC])
        fastqcRelated = createFastqcProcessedFile([
                workflowArtefact: workflowArtefactFastqcRelated,
                dataFile        : createDataFile([
                        seqTrack: createSeqTrack([
                                sample : fastqc1.dataFile.sample,
                                seqType: fastqc1.dataFile.seqType,
                        ]),
                ]),
        ])
        workflowArtefactBamRelated = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFileRelated = createBamFile([
                workflowArtefact: workflowArtefactBamRelated,
                workPackage     : bamFile1.workPackage,
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
    }

    void "fetchBamArtefacts, when called for workflowArtefacts and seqTypes, then return AlignmentArtefactData of expected BamFile"() {
        given:
        setupData()

        AlignmentArtefactData<RoddyBamFile> expected = createAlignmentArtefactDataForRoddyBamFile(bamFile1)

        when:
        List<AlignmentArtefactData<RoddyBamFile>> result = alignmentArtefactService.fetchBamArtefacts(workflowArtefacts, seqTypes)

        then:
        result.size() == 1
        result.first() == expected
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
    }

    void "fetchRelatedBamFileArtefactsForSeqTracks, when called for seqTracks, then return AlignmentArtefactData of expected BamFile"() {
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

        Set<SpeciesWithStrain> speciesWithStrain2 = [fastqc1.dataFile.individual.species] as Set
        ReferenceGenome referenceGenome2 = createReferenceGenome([
                speciesWithStrain: speciesWithStrain2,
                species          : [],
        ])
        createReferenceGenomeSelector([
                referenceGenome: referenceGenome2,
                project        : fastqc1.dataFile.project,
                seqType        : fastqc1.dataFile.seqType,
                workflow       : workflow,
                species        : speciesWithStrain2,
        ])
        ProjectSeqTypeGroup group2 = new ProjectSeqTypeGroup(fastqc1.dataFile.project, fastqc1.dataFile.seqType)

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
                project: fastqc1.dataFile.project,
                seqType: fastqc1.dataFile.seqType,
        ])
        ProjectSeqTypeGroup group2 = new ProjectSeqTypeGroup(fastqc1.dataFile.project, fastqc1.dataFile.seqType)

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
                project            : fastqc1.dataFile.project,
                seqType            : fastqc1.dataFile.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])
        SeqPlatformGroup seqPlatformGroup2 = createSeqPlatformGroup([
                mergingCriteria: mergingCriteria2,
                seqPlatforms   : [fastqc1.dataFile.seqTrack.seqPlatform],
        ])
        ProjectSeqTypeGroup group2 = new ProjectSeqTypeGroup(fastqc1.dataFile.project, fastqc1.dataFile.seqType)

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
                        (fastqc1.dataFile.seqTrack.seqPlatform): seqPlatformGroup2,
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

    void "fetchDataFiles, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        Map<SeqTrack, List<DataFile>> expected = [
                (seqTrack1)                 : seqTrack1.dataFiles,
                (fastqc1.dataFile.seqTrack) : [fastqc1.dataFile],
                (bamFile1.seqTracks.first()): bamFile1.seqTracks.first().dataFiles,
        ]

        when:
        Map<SeqTrack, List<DataFile>> result = alignmentArtefactService.fetchDataFiles(seqTracks)

        then:
        TestCase.assertContainSame(result, expected)
    }

    private SeqTrack createSeqTrackWithTwoDataFileAndSpecies(Map parameters) {
        return createSeqTrackWithTwoDataFile([
                sample: createSample([
                        individual: createIndividual([
                                species: createSpeciesWithStrain(),
                        ]),
                ]),
        ] + parameters)
    }

    private FastqcProcessedFile createFastqcProcessedFileWithSpecies(Map parameters) {
        return createFastqcProcessedFile([
                dataFile: createDataFile([
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
        SeqTrack seqTrack = fastqcProcessedFile.dataFile.seqTrack
        return new AlignmentArtefactData<FastqcProcessedFile>(
                fastqcProcessedFile.workflowArtefact,
                fastqcProcessedFile,
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
