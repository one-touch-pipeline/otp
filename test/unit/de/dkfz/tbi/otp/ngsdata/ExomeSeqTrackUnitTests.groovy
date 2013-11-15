package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*


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

    void testValidateWithKitInfoStateIsKnownAndGivenExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoState= ExomeSeqTrack.KitInfoState.KNOWN
        exomeSeqTrack.exomeEnrichmentKit= new ExomeEnrichmentKit()
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoStateIsKnownAndNoExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoState= ExomeSeqTrack.KitInfoState.KNOWN
        exomeSeqTrack.exomeEnrichmentKit = null
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("exomeEnrichmentKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoStateIsUnknownAndNoExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoState= ExomeSeqTrack.KitInfoState.UNKNOWN
        exomeSeqTrack.exomeEnrichmentKit = null
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoStateIsUnknownAndGivenExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoState= ExomeSeqTrack.KitInfoState.UNKNOWN
        exomeSeqTrack.exomeEnrichmentKit= new ExomeEnrichmentKit()
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("exomeEnrichmentKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNotNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }

    void testValidateWithKitInfoStateIsLaterToCheckAndNoExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoState= ExomeSeqTrack.KitInfoState.LATER_TO_CHECK
        exomeSeqTrack.exomeEnrichmentKit = null
        assertTrue(exomeSeqTrack.validate())
    }

    void testValidateWithKitInfoStateIsLaterToCheckAndGivenExomeEnrichmentKit() {
        exomeSeqTrack.kitInfoState= ExomeSeqTrack.KitInfoState.LATER_TO_CHECK
        exomeSeqTrack.exomeEnrichmentKit= new ExomeEnrichmentKit()
        assertFalse(exomeSeqTrack.validate())
        assertEquals(1, exomeSeqTrack.errors.errorCount)
        assertEquals("de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack", exomeSeqTrack.errors.allErrors[0].objectName)
        assertEquals("exomeEnrichmentKit", exomeSeqTrack.errors.allErrors[0].field)
        assertNotNull(exomeSeqTrack.errors.allErrors[0].rejectedValue)
    }
}
