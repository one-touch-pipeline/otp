package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*


@TestMixin(GrailsUnitTestMixin)
@TestFor(SeqTrack)
@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectCategory,
        ReferenceGenome,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqType,
        SoftwareTool,
])
class SeqTrackUnitTests {

    final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    final String COMMON_PREFIX = "4_NoIndex_L004"

    @Test
    void testIsWithDrawn() {
        SeqTrack seqTrack = new SeqTrack()
        assert null != seqTrack.save(validate: false)
        assertFalse seqTrack.withdrawn

        DataFile dataFile = new DataFile(seqTrack: seqTrack, fileWithdrawn: false)
        assert null != dataFile.save(validate: false)
        assertFalse seqTrack.withdrawn

        dataFile = new DataFile(seqTrack: seqTrack, fileWithdrawn: false)
        assert null != dataFile.save(validate: false)
        assertFalse seqTrack.withdrawn

        dataFile.fileWithdrawn = true
        assert null != dataFile.save(validate: false)
        assertTrue seqTrack.withdrawn
    }

    @Test
    void testGetProject() {
        Project project = DomainFactory.createProject()
        assert null != project.save(validate: false)

        Individual individual = new Individual (project: project)
        assert null != individual.save(validate: false)

        Sample sample = new Sample(individual: individual)
        assert null != sample.save(validate: false)

        SeqTrack seqTrack = new SeqTrack(sample: sample)
        assert null != seqTrack.save(validate: false)

        assertEquals(seqTrack.project, project)
    }



    @Test
    void testValidateWithKitInfoReliabilityIsKnownAndGivenLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability = InformationReliability.KNOWN
        seqTrack.libraryPreparationKit = new LibraryPreparationKit()
        assert seqTrack.validate()
    }

    @Test
    void testValidateWithKitInfoReliabilityIsInferredAndGivenLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability = InformationReliability.INFERRED
        seqTrack.libraryPreparationKit = new LibraryPreparationKit()

        assert seqTrack.validate()
    }

    @Test
    void testValidateWithKitInfoReliabilityIsKnownAndNoLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability = InformationReliability.KNOWN
        seqTrack.libraryPreparationKit = null

        TestCase.assertValidateError(seqTrack, "libraryPreparationKit", "validator.invalid", null)
    }

    @Test
    void testValidateWithKitInfoReliabilityIsInferredAndNoLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability = InformationReliability.INFERRED
        seqTrack.libraryPreparationKit = null

        TestCase.assertValidateError(seqTrack, "libraryPreparationKit", "validator.invalid", null)
    }

    @Test
    void testValidateWithKitInfoReliabilityIsUnknownAndNoLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
        seqTrack.libraryPreparationKit = null

        assert seqTrack.validate()
    }

    @Test
    void testValidateWithKitInfoReliabilityIsUnknownAndGivenLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
        seqTrack.libraryPreparationKit = new LibraryPreparationKit()

        TestCase.assertValidateError(seqTrack, "libraryPreparationKit", "validator.invalid", seqTrack.libraryPreparationKit)
    }

    @Test
    void testValidateWithKitInfoReliabilityIsUnknownUnverifiedAndNoLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability= InformationReliability.UNKNOWN_UNVERIFIED
        seqTrack.libraryPreparationKit = null

        assertTrue(seqTrack.validate())
    }

    @Test
    void testValidateWithKitInfoReliabilityIsUnknownUnverifiedAndGivenLibraryPreparationKit() {
        SeqTrack seqTrack = createSeqTrack()
        seqTrack.kitInfoReliability= InformationReliability.UNKNOWN_UNVERIFIED
        seqTrack.libraryPreparationKit = new LibraryPreparationKit()

        TestCase.assertValidateError(seqTrack, "libraryPreparationKit", "validator.invalid", seqTrack.libraryPreparationKit)
    }


    @Test
    void testNormalizeLibraryName_InputNull_MustReturnNull() {
        assertNull(SeqTrack.normalizeLibraryName(null))
    }

    @Test
    void testNormalizeLibraryName_NormalizeInput() {
        assert "1" == SeqTrack.normalizeLibraryName("lib_1")
        assert "1" == SeqTrack.normalizeLibraryName("lib-1")
        assert "0" == SeqTrack.normalizeLibraryName("lib000")
        assert "1" == SeqTrack.normalizeLibraryName("lib0001")
        assert "1" == SeqTrack.normalizeLibraryName("library1")
    }


    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_FirstStringNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqTrack.getLongestCommonPrefixBeforeLastUnderscore(null, SECOND_DATAFILE_NAME)
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_SecondStringNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqTrack.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, null)
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_FirstStringEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqTrack.getLongestCommonPrefixBeforeLastUnderscore("", SECOND_DATAFILE_NAME)
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_SecondStringEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqTrack.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, "")
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_StringsAreEqual() {
        assert "4_NoIndex_L004_R1_complete" ==
                SeqTrack.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, FIRST_DATAFILE_NAME)
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_PrefixIsEqual() {
        assert COMMON_PREFIX ==
                SeqTrack.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, SECOND_DATAFILE_NAME)
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_NoUnderScoreInStrings() {
        assert "NoUnderScoreR" ==
                SeqTrack.getLongestCommonPrefixBeforeLastUnderscore("NoUnderScoreR1.tar.gz", "NoUnderScoreR2.tar.gz")
    }


    @Test
    void testGetReadGroupName_PAIRED_OnlyOneDataFile_ShouldFail() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(DomainFactory.createMergingWorkPackage())
        DataFile.findAllBySeqTrack(seqTrack)[0].delete(flush: true)
        TestCase.shouldFail(AssertionError) {
            seqTrack.getReadGroupName()
        }
    }

    @Test
    void testGetReadGroupName_PAIRED_SameDataFileName_ShouldFail() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(DomainFactory.createMergingWorkPackage())
        assert DomainFactory.createSequenceDataFile([seqTrack: seqTrack])
        TestCase.shouldFail(AssertionError) {
            seqTrack.getReadGroupName()
        }
    }

    @Test
    void testGetReadGroupName_PAIRED_AllFine() {
        MergingWorkPackage mwp = DomainFactory.createMergingWorkPackage(seqType: DomainFactory.createRnaPairedSeqType())
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(mwp)
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        dataFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        dataFiles[1].vbpFileName = SECOND_DATAFILE_NAME
        assert "run${seqTrack.run.name}_${COMMON_PREFIX}" == seqTrack.getReadGroupName()
    }

    @Test
    void testGetReadGroupName_SINGLE_AllFine() {
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithOneDataFile([:], [vbpFileName: "fileName.fastq.gz"])
        assert "run${seqTrack1.run.name}_${'fileName'}" == seqTrack1.getReadGroupName()
    }


    private SeqTrack createSeqTrack() throws Exception {
        return new SeqTrack(
                        laneId: "lane",
                        run: new Run(),
                        sample: new Sample(),
                        seqType: new SeqType(),
                        seqPlatform: new SeqPlatform(),
                        pipelineVersion: new SoftwareTool()
                        )
    }
}
