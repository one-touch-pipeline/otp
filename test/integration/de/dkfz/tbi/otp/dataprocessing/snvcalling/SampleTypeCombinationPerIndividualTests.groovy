package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static org.junit.Assert.*
import grails.validation.ValidationException
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Individual.Type

class SampleTypeCombinationPerIndividualTests extends GroovyTestCase {

    Project project
    Individual individual
    SampleType sampleType1
    SampleType sampleType2
    SeqType seqType

    TestData testData

    @Before
    void setUp() {
        testData = new TestData()

        testData.createObjects()

        project = new Project(
                name: "project",
                dirName: "/dirName/",
                realmName: "DKFZ",
                )
        project.save()

        individual = new Individual(
                project: project,
                pid: "pid",
                mockPid: "mockPid",
                mockFullName: "mockFullName",
                type: Type.REAL
                )
        individual.save()

        sampleType1 = new SampleType(
                name: "BLOOD"
                )
        sampleType1.save()

        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)

        sampleType2 = new SampleType(
                name: "CONTROL"
                )
        sampleType2.save()

        seqType = new SeqType(
                name: "EXOME",
                libraryLayout: "PAIRED",
                dirName: "/tmp"
                )
        seqType.save()
    }

    @After
    void tearDown() {
        individual = null
        sampleType1 = null
        sampleType2 = null
        seqType = null
        project = null
        testData = null
    }

    @Test
    void testAllCorrect() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        sampleCombinationPerIndividual.save()
    }

    @Test
    void testPairsWithSameSampleTypes() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType1,
                seqType: seqType
                )
        shouldFail ValidationException, {
            sampleCombinationPerIndividual.save()
        }
    }

    @Test
    void testPairsUniquePerIndividual() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual1 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        sampleCombinationPerIndividual1.save()
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual2 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        shouldFail ValidationException, {
            sampleCombinationPerIndividual2.save()
        }
    }

    @Test
    void testPairsUniquePerIndividualInBothDirections() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual1 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        sampleCombinationPerIndividual1.save()
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual2 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType2,
                sampleType2: sampleType1,
                seqType: seqType
                )
        shouldFail ValidationException, {
            sampleCombinationPerIndividual2.save()
        }
    }

    @Test
    void testGetLatestProcessedMergedBamFileForSampleTypeSeveralPasses() {
        ProcessedMergedBamFile processedMergedBamFile1_A = createProcessedMergedBamFile("1")
        processedMergedBamFile1_A.save()
        ProcessedMergedBamFile processedMergedBamFile2 = createProcessedMergedBamFile("2")
        processedMergedBamFile2.save()

        SampleType sampleType1 = processedMergedBamFile1_A.sample.sampleType
        SampleType sampleType2 = processedMergedBamFile2.sample.sampleType

        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                seqType: seqType,
                sampleType1: sampleType1,
                sampleType2: sampleType2
                )
        sampleTypeCombinationPerIndividual.save()

        assertEquals(processedMergedBamFile1_A, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType1))
        assertEquals(processedMergedBamFile2, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType2))


        SeqTrack seqTrack1_B = testData.createSeqTrack([sample: processedMergedBamFile1_A.sample, seqType: seqType])
        seqTrack1_B.save()

        AlignmentPass alignmentPass1_B = testData.createAlignmentPass([seqTrack: seqTrack1_B])
        alignmentPass1_B.save()

        ProcessedBamFile processedBamFile1_B = testData.createProcessedBamFile([alignmentPass: alignmentPass1_B,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED])
        processedBamFile1_B.save()

        MergingSetAssignment mergingSetAssignment1_B1 = new MergingSetAssignment(
                mergingSet: processedMergedBamFile1_A.mergingSet,
                bamFile: processedMergedBamFile1_A
                )
        mergingSetAssignment1_B1.save()

        MergingSetAssignment mergingSetAssignment1_B2 = new MergingSetAssignment(
                mergingSet: processedMergedBamFile1_A.mergingSet,
                bamFile: processedBamFile1_B
                )
        mergingSetAssignment1_B2.save()

        MergingPass mergingPass1_B = testData.createMergingPass([mergingSet: processedMergedBamFile1_A.mergingSet, identifier: 1])
        mergingPass1_B.save()

        ProcessedMergedBamFile processedMergedBamFile1_B = testData.createProcessedMergedBamFile([mergingPass: mergingPass1_B,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            fileOperationStatus: FileOperationStatus.PROCESSED])
        processedMergedBamFile1_B.save()

        assertEquals(processedMergedBamFile1_B, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType1))
        assertEquals(processedMergedBamFile2, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType2))

        processedMergedBamFile1_B.withdrawn = true

        assertNull(sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType1))
        assertEquals(processedMergedBamFile2, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType2))
    }


    @Test
    void testGetLatestProcessedMergedBamFileForSampleTypeBamFilesFromDifferentSeqTypes() {
        ProcessedMergedBamFile processedMergedBamFile1_A = createProcessedMergedBamFile("1")
        processedMergedBamFile1_A.save()
        ProcessedMergedBamFile processedMergedBamFile1_B = createProcessedMergedBamFile("2")
        processedMergedBamFile1_B.save()

        ProcessedMergedBamFile processedMergedBamFile2_A = createProcessedMergedBamFile("3")
        processedMergedBamFile2_A.save()
        ProcessedMergedBamFile processedMergedBamFile2_B = createProcessedMergedBamFile("4")
        processedMergedBamFile2_B.save()

        //change seqTypes of bam file 2_A and 2_B so that they are different from 1_A and 1_B
        SeqType otherSeqType = testData.seqType

        processedMergedBamFile2_A.containedSeqTracks.each { SeqTrack seqTrack ->
            seqTrack.seqType = otherSeqType
            seqTrack.save()
        }
        processedMergedBamFile2_A.mergingWorkPackage.seqType = otherSeqType
        processedMergedBamFile2_A.mergingWorkPackage.save()

        processedMergedBamFile2_B.containedSeqTracks.each { SeqTrack seqTrack ->
            seqTrack.seqType = otherSeqType
            seqTrack.save()
        }
        processedMergedBamFile2_B.mergingWorkPackage.seqType = otherSeqType
        processedMergedBamFile2_B.mergingWorkPackage.save()

        //change sample Types of bam file 2_A and 2_B so that they match with 1_A and 1_B
        SampleType sampleTypeA = processedMergedBamFile1_A.sample.sampleType
        SampleType sampleTypeB = processedMergedBamFile1_B.sample.sampleType

        processedMergedBamFile2_A.sample.sampleType = sampleTypeA
        processedMergedBamFile2_A.sample.save()

        processedMergedBamFile2_B.sample.sampleType = sampleTypeB
        processedMergedBamFile2_B.sample.save()

        SampleTypePerProject.build(project: project, sampleType: sampleTypeA, category: SampleType.Category.DISEASE)

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                seqType: seqType,
                sampleType1: sampleTypeA,
                sampleType2: sampleTypeB
                )
        sampleTypeCombinationPerIndividual.save()

        assertEquals(processedMergedBamFile1_A, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeA))
        assertEquals(processedMergedBamFile1_B, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeB))

        sampleTypeCombinationPerIndividual.seqType = otherSeqType
        sampleTypeCombinationPerIndividual.save()

        assertEquals(processedMergedBamFile2_A, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeA))
        assertEquals(processedMergedBamFile2_B, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeB))
    }


    @Test
    void testGetLatestProcessedMergedBamFileForSampleTypeBamFilesFromDifferentIndividuals() {
        ProcessedMergedBamFile processedMergedBamFile1_A = createProcessedMergedBamFile("1")
        processedMergedBamFile1_A.save()
        ProcessedMergedBamFile processedMergedBamFile1_B = createProcessedMergedBamFile("2")
        processedMergedBamFile1_B.save()

        ProcessedMergedBamFile processedMergedBamFile2_A = createProcessedMergedBamFile("3")
        processedMergedBamFile2_A.save()
        ProcessedMergedBamFile processedMergedBamFile2_B = createProcessedMergedBamFile("4")
        processedMergedBamFile2_B.save()

        //change sample Types of bam file 2_A and 2_B so that they match with 1_A and 1_B
        SampleType sampleTypeA = processedMergedBamFile1_A.sample.sampleType
        SampleType sampleTypeB = processedMergedBamFile1_B.sample.sampleType

        processedMergedBamFile2_A.sample.sampleType = sampleTypeA
        processedMergedBamFile2_A.sample.save()

        processedMergedBamFile2_B.sample.sampleType = sampleTypeB
        processedMergedBamFile2_B.sample.save()

        //change individuals of bam file 2_A and 2_B so that they are different from 1_A and 1_B
        Individual otherIndividual = testData.individual
        processedMergedBamFile2_A.sample.individual = otherIndividual
        processedMergedBamFile2_A.save()
        processedMergedBamFile2_B.sample.individual = otherIndividual
        processedMergedBamFile2_B.save()

        SampleTypePerProject.build(project: project, sampleType: sampleTypeA, category: SampleType.Category.DISEASE)

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                seqType: seqType,
                sampleType1: sampleTypeA,
                sampleType2: sampleTypeB
                )
        sampleTypeCombinationPerIndividual.save()

        assertEquals(processedMergedBamFile1_A, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeA))
        assertEquals(processedMergedBamFile1_B, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeB))

        SampleTypePerProject.build(project: otherIndividual.project, sampleType: sampleTypeA, category: SampleType.Category.DISEASE)

        sampleTypeCombinationPerIndividual.individual = otherIndividual
        sampleTypeCombinationPerIndividual.save()

        assertEquals(processedMergedBamFile2_A, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeA))
        assertEquals(processedMergedBamFile2_B, sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeB))

    }

    @Test
    void testSetNeedsProcessingTrue() {
        testSetNeedsProcessing(true)
    }

    @Test
    void testSetNeedsProcessingFalse() {
        testSetNeedsProcessing(false)
    }

    private void testSetNeedsProcessing(final boolean needsProcessing) {
        final SampleTypeCombinationPerIndividual nonPersistedCombination = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType,
                needsProcessing: needsProcessing,  // Tests that the instance is persisted even if it already has the correct value.
        )
        final SampleTypeCombinationPerIndividual persistedCombination = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: SeqType.build(),
                needsProcessing: !needsProcessing,
        )
        assert persistedCombination.save()

        SampleTypeCombinationPerIndividual.setNeedsProcessing([nonPersistedCombination, persistedCombination], needsProcessing)

        assert nonPersistedCombination.needsProcessing == needsProcessing
        assert nonPersistedCombination.id
        assert persistedCombination.needsProcessing == needsProcessing
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(String identifier) {
        SampleType sampleType = testData.createSampleType([name: "SampleType"+identifier])
        sampleType.save()

        Sample sample = testData.createSample([individual: individual, sampleType: sampleType])
        sample.save()

        SeqTrack seqTrack = testData.createSeqTrack([sample: sample, seqType: seqType])
        seqTrack.save()

        AlignmentPass alignmentPass = testData.createAlignmentPass([seqTrack: seqTrack])
        alignmentPass.save()

        ProcessedBamFile processedBamFile = testData.createProcessedBamFile([alignmentPass: alignmentPass,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED])
        processedBamFile.save()

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage([sample: sample, seqType: seqType])
        mergingWorkPackage.save()

        MergingSet mergingSet = testData.createMergingSet([mergingWorkPackage: mergingWorkPackage, status: State.PROCESSED])
        mergingSet.save()

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                mergingSet: mergingSet,
                bamFile: processedBamFile
                )
        mergingSetAssignment.save()

        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        mergingPass.save()

        return  testData.createProcessedMergedBamFile([mergingPass: mergingPass,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            fileOperationStatus: FileOperationStatus.PROCESSED])
    }
}
