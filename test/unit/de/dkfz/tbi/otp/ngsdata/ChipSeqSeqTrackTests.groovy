package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

@TestMixin(GrailsUnitTestMixin)
@TestFor(ChipSeqSeqTrack)
@Mock([ChipSeqSeqTrack, AntibodyTarget])
class ChipSeqSeqTrackTests {

    static final String ANTIBODY = "antibodyName"

    static final AntibodyTarget ANTIBODY_TARGET = new AntibodyTarget(name: "antibodyTargetName")

    ChipSeqSeqTrack chipSeqSeqTrack

    @Before
    void setUp() throws Exception {
        chipSeqSeqTrack =  new ChipSeqSeqTrack(
                        laneId: "lane",
                        run: new Run(),
                        sample: new Sample(),
                        seqType: new SeqType(name: SeqTypeNames.CHIP_SEQ.seqTypeName),
                        seqPlatform: new SeqPlatform(),
                        pipelineVersion: new SoftwareTool()
                        )
    }

    @After
    void tearDown() throws Exception {
        chipSeqSeqTrack = null
    }

    @Test
    void testNullableAntibodyAndValidAntibodyTarget() {
        chipSeqSeqTrack.antibodyTarget = ANTIBODY_TARGET
        assertTrue chipSeqSeqTrack.validate()
    }

    @Test
    void testNotNullAntibodyAndValidAntibodyTarget() {
        chipSeqSeqTrack.antibodyTarget = ANTIBODY_TARGET
        chipSeqSeqTrack.antibody = ANTIBODY
        assertTrue chipSeqSeqTrack.validate()
    }

    @Test
    void testNullAntibodyTarget() {
        chipSeqSeqTrack.antibody = ANTIBODY
        assertFalse chipSeqSeqTrack.validate()
    }
}
