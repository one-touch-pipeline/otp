package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*


@TestMixin(GrailsUnitTestMixin)
@TestFor(SeqTrack)
@Mock([DataFile, Sample, Individual, Project])
class SeqTrackUnitTests {

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
        Project project = TestData.createProject()
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
