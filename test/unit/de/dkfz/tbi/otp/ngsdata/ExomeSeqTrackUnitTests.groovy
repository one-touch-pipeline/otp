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

    void testValidateWithKitInfoReliabilityIsKnownAndGivenExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.KNOWN
        exomeSeqTrack.exomeEnrichmentKit= new ExomeEnrichmentKit()
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsInferredAndGivenExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.INFERRED
        exomeSeqTrack.exomeEnrichmentKit= new ExomeEnrichmentKit()
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsKnownAndNoExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.KNOWN
        exomeSeqTrack.exomeEnrichmentKit = null
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("exomeEnrichmentKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoReliabilityIsInferredAndNoExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.INFERRED
        exomeSeqTrack.exomeEnrichmentKit = null
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("exomeEnrichmentKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoReliabilityIsUnknownAndNoExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_VERIFIED
        exomeSeqTrack.exomeEnrichmentKit = null
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsUnknownAndGivenExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_VERIFIED
        exomeSeqTrack.exomeEnrichmentKit= new ExomeEnrichmentKit()
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("exomeEnrichmentKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNotNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoReliabilityIsUnknownUnverifiedAndNoExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_UNVERIFIED
        exomeSeqTrack.exomeEnrichmentKit = null
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoReliabilityIsUnknownUnverifiedAndGivenExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoReliability= InformationReliability.UNKNOWN_UNVERIFIED
        exomeSeqTrack.exomeEnrichmentKit= new ExomeEnrichmentKit()
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("exomeEnrichmentKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNotNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }
}
