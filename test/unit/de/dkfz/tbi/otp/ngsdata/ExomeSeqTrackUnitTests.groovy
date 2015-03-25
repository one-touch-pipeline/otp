package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*
import de.dkfz.tbi.otp.InformationReliability


@TestMixin(GrailsUnitTestMixin)
@TestFor(ExomeSeqTrack)
@Mock(ExomeSeqTrack)
class ExomeSeqTrackUnitTests {

    ExomeSeqTrack exomeSeqTrack

    @Before
    public void setUp() throws Exception {
        exomeSeqTrack =  new ExomeSeqTrack(
                        laneId: "lane",
                        run: new Run(),
                        sample: new Sample(),
                        seqType: new SeqType(name: SeqTypeNames.EXOME.seqTypeName),
                        seqPlatform: new SeqPlatform(),
                        pipelineVersion: new SoftwareTool()
                        )
    }

    @After
    public void tearDown() throws Exception {
        exomeSeqTrack = null
    }

    void testValidateWithKitInfoReliabilityIsKnownAndGivenLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.KNOWN
        exomeSeqTrack.libraryPreparationKit= new LibraryPreparationKit()
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsInferredAndGivenLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.INFERRED
        exomeSeqTrack.libraryPreparationKit= new LibraryPreparationKit()
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsKnownAndNoLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.KNOWN
        exomeSeqTrack.libraryPreparationKit = null
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("libraryPreparationKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoReliabilityIsInferredAndNoLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.INFERRED
        exomeSeqTrack.libraryPreparationKit = null
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("libraryPreparationKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoReliabilityIsUnknownAndNoLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_VERIFIED
        exomeSeqTrack.libraryPreparationKit = null
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsUnknownAndGivenLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_VERIFIED
        exomeSeqTrack.libraryPreparationKit= new LibraryPreparationKit()
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("libraryPreparationKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNotNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoReliabilityIsUnknownUnverifiedAndNoLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_UNVERIFIED
        exomeSeqTrack.libraryPreparationKit = null
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsUnknownUnverifiedAndGivenLibraryPreparationKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_UNVERIFIED
        exomeSeqTrack.libraryPreparationKit= new LibraryPreparationKit()
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("libraryPreparationKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNotNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }
}
