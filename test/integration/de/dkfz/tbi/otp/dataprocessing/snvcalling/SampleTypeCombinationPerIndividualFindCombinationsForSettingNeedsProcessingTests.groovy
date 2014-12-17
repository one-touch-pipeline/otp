package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.utils.HelperUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ThreadUtils

class SampleTypeCombinationPerIndividualFindCombinationsForSettingNeedsProcessingTests {

    SnvCallingInstanceTestData testData
    SampleTypeCombinationPerIndividual samplePair
    Project project
    SampleType sampleType1
    SampleTypePerProject sampleType1Stpp

    @Before
    void before() {
        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()

        samplePair = testData.sampleTypeCombination
        samplePair.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair.save(failOnError: true, flush: true)

        project = samplePair.project
        sampleType1 = testData.sampleTypeCombination.sampleType1
        sampleType1Stpp = exactlyOneElement(SampleTypePerProject.findAllWhere(project: project, sampleType: sampleType1))
    }

    @Test
    void testProcessingStatusAlreadyNeedsProcessing() {
        samplePair.processingStatus = ProcessingStatus.NEEDS_PROCESSING
        assert samplePair.save(failOnError: true)
        assertFindsNothing()
    }

    @Test
    void testProcessingStatusDisabled() {
        samplePair.processingStatus = ProcessingStatus.DISABLED
        assert samplePair.save(failOnError: true)
        assertFindsNothing()
    }

    @Test
    void testStppProjectMismatch() {
        sampleType1Stpp.project = Project.build()
        assert sampleType1Stpp.save(failOnError: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testSampleType1CategoryControl() {
        sampleType1Stpp.category = SampleType.Category.CONTROL
        assert sampleType1Stpp.save(failOnError: true)
        assertFindsNothing()
    }

    @Test
    void testSampleType1CategoryUnknown() {
        sampleType1Stpp.delete()
        assertFindsNothing()
    }

    @Test
    void testNoSnvCallingInstanceExists() {
        assertFindsOne()
    }

    @Test
    void testTwoResults() {
        testData.sampleTypeCombination2.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
        assert testData.sampleTypeCombination2.save(failOnError: true)
        assert TestCase.containSame(
                SampleTypeCombinationPerIndividual.findCombinationsForSettingNeedsProcessing(),
                [samplePair, testData.sampleTypeCombination2]
        )
    }

    @Test
    void testSnvCallingInstanceForOtherSamplePairExists() {
        testData.createAndSaveSnvCallingInstance(
                sampleTypeCombination: testData.sampleTypeCombination2,
                sampleType1BamFile: testData.bamFileTumor2,
        )
        assertFindsOne()
    }

    @Test
    void testSnvCallingInstanceAlreadyExists() {
        final SnvCallingInstance instance = testData.createAndSaveSnvCallingInstance()
        // one SnvCallingInstance without any SnvJobResult
        assertFindsNothing()

        final SnvJobResult callingResult = testData.createAndSaveSnvJobResult(instance, SnvCallingStep.CALLING)
        // one SnvCallingInstance with one non-withdrawn SnvJobResult
        assertFindsNothing()

        final SnvJobResult annotationResult = testData.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_ANNOTATION, callingResult)
        // one SnvCallingInstance with two non-withdrawn SnvJobResults
        assertFindsNothing()

        annotationResult.withdrawn = true
        assert annotationResult.save(failOnError: true)
        // one SnvCallingInstance with one non-withdrawn and one withdrawn SnvJobResult
        assertFindsOne()

        testData.createAndSaveSnvCallingInstance(instanceName: HelperUtils.uniqueString)
        // one SnvCallingInstance with one non-withdrawn and one withdrawn SnvJobResult and one without any SnvJobResult
        assertFindsNothing()
    }

    @Test
    void testLaterSampleType1DataFileExists() {
        createSnvCallingInstanceAndLaterDataFile(sampleType1)
        assertFindsOne()
    }

    @Test
    void testLaterSampleType2DataFileExists() {
        createSnvCallingInstanceAndLaterDataFile(samplePair.sampleType2)
        assertFindsOne()
    }

    @Test
    void testLaterDataFileWithOtherFileTypeExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.fileType = FileType.build()
        assert dataFile.save(failOnError: true)
        assertFindsNothing()
    }

    @Test
    void testLaterWithdrawnDataFileExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.fileWithdrawn = true
        assert dataFile.save(failOnError: true)
        assertFindsNothing()
    }

    @Test
    void testLaterDataFileForOtherIndividualExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.seqTrack.sample = Sample.build(individual: Individual.build(project: project), sampleType: sampleType1)
        assert dataFile.seqTrack.save(failOnError: true)
        assertFindsNothing()
    }

    @Test
    void testLaterDataFileForOtherSampleTypeExists() {
        createSnvCallingInstanceAndLaterDataFile(Sample.build(individual: samplePair.individual, sampleType: SampleType.build()))
        assertFindsNothing()
    }

    @Test
    void testLaterDataFileForOtherSeqTypeExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.seqTrack.seqType = SeqType.build()
        assert dataFile.seqTrack.save(failOnError: true)
        assertFindsNothing()
    }

    private DataFile createSnvCallingInstanceAndLaterDataFile(final SampleType sampleType) {
        return createSnvCallingInstanceAndLaterDataFile(exactlyOneElement(Sample.findAllBySampleType(sampleType)))
    }

    private DataFile createSnvCallingInstanceAndLaterDataFile(final Sample sample) {
        final SnvCallingInstance instance = testData.createAndSaveSnvCallingInstance()
        final SeqTrack seqTrack = SeqTrack.findWhere(sample: sample, seqType: instance.seqType)
        assert ThreadUtils.waitFor( { System.currentTimeMillis() > instance.latestDataFileCreationDate.time }, 1, 1)
        final DataFile dataFile = DomainFactory.buildSequenceDataFile(seqTrack: seqTrack)
        assert dataFile.dateCreated > instance.latestDataFileCreationDate
        return dataFile
    }

    void assertFindsNothing() {
        assert SampleTypeCombinationPerIndividual.findCombinationsForSettingNeedsProcessing().empty
    }

    void assertFindsOne() {
        assert exactlyOneElement(SampleTypeCombinationPerIndividual.findCombinationsForSettingNeedsProcessing()) == samplePair
    }
}
