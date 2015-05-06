package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static org.junit.Assert.*

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import grails.validation.ValidationException
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Individual.Type

class SamplePairTests extends GroovyTestCase {

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

        project = TestData.createProject(
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
        SamplePair samplePair = new SamplePair(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        samplePair.save()
    }

    @Test
    void testPairsWithSameSampleTypes() {
        SamplePair samplePair = new SamplePair(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType1,
                seqType: seqType
                )
        shouldFail ValidationException, {
            samplePair.save()
        }
    }

    @Test
    void testPairsUniquePerIndividual() {
        SamplePair samplePair1 = new SamplePair(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        samplePair1.save()
        SamplePair samplePair2 = new SamplePair(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        shouldFail ValidationException, {
            samplePair2.save()
        }
    }

    @Test
    void testPairsUniquePerIndividualInBothDirections() {
        SamplePair samplePair1 = new SamplePair(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType
                )
        samplePair1.save()
        SamplePair samplePair2 = new SamplePair(
                individual: individual,
                sampleType1: sampleType2,
                sampleType2: sampleType1,
                seqType: seqType
                )
        shouldFail ValidationException, {
            samplePair2.save()
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

        SamplePair samplePair = new SamplePair(
                individual: individual,
                seqType: seqType,
                sampleType1: sampleType1,
                sampleType2: sampleType2
                )
        samplePair.save()

        assert processedMergedBamFile1_A.isMostRecentBamFile()
        assertEquals(processedMergedBamFile1_A, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType1))
        assert processedMergedBamFile2.isMostRecentBamFile()
        assertEquals(processedMergedBamFile2, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType2))


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

        assert processedMergedBamFile1_B.isMostRecentBamFile()
        assertEquals(processedMergedBamFile1_B, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType1))
        assert processedMergedBamFile2.isMostRecentBamFile()
        assertEquals(processedMergedBamFile2, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType2))

        final MergingSet mergingSet3 = MergingSet.build(mergingWorkPackage: processedMergedBamFile1_A.mergingWorkPackage, identifier: 1)
        final MergingPass mergingPass3 = MergingPass.build(mergingSet: mergingSet3, identifier: 0)
        final ProcessedMergedBamFile bamFile3 = ProcessedMergedBamFile.build(mergingPass: mergingPass3)

        assert bamFile3.mergingPass.identifier < processedMergedBamFile1_B.mergingPass.identifier
        assert bamFile3.mergingSet.identifier > processedMergedBamFile1_B.mergingSet.identifier
        assert !processedMergedBamFile1_B.isMostRecentBamFile()
        assert bamFile3.isMostRecentBamFile()
        assertEquals(bamFile3, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType1))

        bamFile3.withdrawn = true

        assertNull(samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType1))
        assert processedMergedBamFile2.isMostRecentBamFile()
        assertEquals(processedMergedBamFile2, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleType2))
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

        Sample sampleA = processedMergedBamFile1_A.sample
        Sample sampleB = processedMergedBamFile1_B.sample

        processedMergedBamFile2_A.mergingWorkPackage.sample = sampleA
        processedMergedBamFile2_A.save()

        processedMergedBamFile2_B.mergingWorkPackage.sample = sampleB
        processedMergedBamFile2_B.save()

        SampleTypePerProject.build(project: project, sampleType: sampleA.sampleType, category: SampleType.Category.DISEASE)

        SamplePair samplePair = new SamplePair(
                individual: individual,
                seqType: seqType,
                sampleType1: sampleA.sampleType,
                sampleType2: sampleB.sampleType
                )
        samplePair.save()

        assert processedMergedBamFile1_A.isMostRecentBamFile()
        assertEquals(processedMergedBamFile1_A, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleA.sampleType))
        assert processedMergedBamFile1_B.isMostRecentBamFile()
        assertEquals(processedMergedBamFile1_B, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleB.sampleType))

        samplePair.seqType = otherSeqType
        samplePair.save()

        assert processedMergedBamFile2_A.isMostRecentBamFile()
        assertEquals(processedMergedBamFile2_A, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleA.sampleType))
        assert processedMergedBamFile2_B.isMostRecentBamFile()
        assertEquals(processedMergedBamFile2_B, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleB.sampleType))
    }


    @Test
    void testGetLatestProcessedMergedBamFileForSampleTypeBamFilesFromDifferentIndividuals() {
        ProcessedMergedBamFile processedMergedBamFile1_A = createProcessedMergedBamFile("1")
        processedMergedBamFile1_A.save(failOnError: true, flush: true)
        ProcessedMergedBamFile processedMergedBamFile1_B = createProcessedMergedBamFile("2")
        processedMergedBamFile1_B.save(failOnError: true, flush: true)

        ProcessedMergedBamFile processedMergedBamFile2_A = createProcessedMergedBamFile("3")
        processedMergedBamFile2_A.save(failOnError: true, flush: true)
        ProcessedMergedBamFile processedMergedBamFile2_B = createProcessedMergedBamFile("4")
        processedMergedBamFile2_B.save(failOnError: true, flush: true)

        //change individuals of bam file 2_A and 2_B so that they are different from 1_A and 1_B
        Individual otherIndividual = testData.individual
        processedMergedBamFile2_A.sample.individual = otherIndividual
        processedMergedBamFile2_A.save(failOnError: true, flush: true)
        processedMergedBamFile2_B.sample.individual = otherIndividual
        processedMergedBamFile2_B.save(failOnError: true, flush: true)

        //change sample Types of bam file 2_A and 2_B so that they match with 1_A and 1_B
        SampleType sampleTypeA = processedMergedBamFile1_A.sample.sampleType
        SampleType sampleTypeB = processedMergedBamFile1_B.sample.sampleType

        processedMergedBamFile2_A.sample.sampleType = sampleTypeA
        processedMergedBamFile2_A.sample.save()

        processedMergedBamFile2_B.sample.sampleType = sampleTypeB
        processedMergedBamFile2_B.sample.save()

        SampleTypePerProject.build(project: project, sampleType: sampleTypeA, category: SampleType.Category.DISEASE)

        SamplePair samplePair = new SamplePair(
                individual: individual,
                seqType: seqType,
                sampleType1: sampleTypeA,
                sampleType2: sampleTypeB
                )
        samplePair.save(failOnError: true, flush: true)

        assert processedMergedBamFile1_A.isMostRecentBamFile()
        assertEquals(processedMergedBamFile1_A, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeA))
        assert processedMergedBamFile1_B.isMostRecentBamFile()
        assertEquals(processedMergedBamFile1_B, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeB))

        SampleTypePerProject.build(project: otherIndividual.project, sampleType: sampleTypeA, category: SampleType.Category.DISEASE)

        samplePair.individual = otherIndividual
        samplePair.save()

        assert processedMergedBamFile2_A.isMostRecentBamFile()
        assertEquals(processedMergedBamFile2_A, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeA))
        assert processedMergedBamFile2_B.isMostRecentBamFile()
        assertEquals(processedMergedBamFile2_B, samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(sampleTypeB))

    }

    @Test
    void testSetProcessingStatusNeedsProcessing() {
        testSetNeedsProcessing(ProcessingStatus.NEEDS_PROCESSING)
    }

    @Test
    void testSetProcessingStatusNoProcessingNeeded() {
        testSetNeedsProcessing(ProcessingStatus.NO_PROCESSING_NEEDED)
    }

    @Test
    void testSetProcessingStatusDisabled() {
        testSetNeedsProcessing(ProcessingStatus.DISABLED)
    }

    private void testSetNeedsProcessing(final ProcessingStatus processingStatus) {
        final SamplePair nonPersistedSamplePair = new SamplePair(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: seqType,
                processingStatus: processingStatus,  // Tests that the instance is persisted even if it already has the correct value.
        )
        final SamplePair persistedSamplePair = new SamplePair(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: sampleType2,
                seqType: SeqType.build(),
                processingStatus: processingStatus == ProcessingStatus.NEEDS_PROCESSING ? ProcessingStatus.NO_PROCESSING_NEEDED : ProcessingStatus.NEEDS_PROCESSING,
        )
        assert persistedSamplePair.save()

        SamplePair.setProcessingStatus([nonPersistedSamplePair, persistedSamplePair], processingStatus)

        assert nonPersistedSamplePair.processingStatus == processingStatus
        assert nonPersistedSamplePair.id
        assert persistedSamplePair.processingStatus == processingStatus
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

        MergingSet mergingSet = testData.createMergingSet([mergingWorkPackage: processedBamFile.mergingWorkPackage, status: State.PROCESSED])
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
