package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*

@TestFor(SeqCenterService)
@Build([
        SeqCenter
])
class SeqCenterServiceUnitTests {

    SeqCenterService seqCenterService

    final static String SEQ_CENTER ="SeqCenter"

    final static String DIFFERENT_SEQ_CENTER ="DifferentSeqCenter"

    final static String SEQ_CENTER_DIR = "SeqCenterDir"

    final static String DIFFERENT_SEQ_CENTER_DIR = "DifferentSeqCenterDir"

    @Before
    void setUp() throws Exception {
        seqCenterService = new SeqCenterService()
    }


    @After
    void tearDown() throws Exception {
        seqCenterService = null
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterAndSeqCenterDir() {
        assertEquals(
                seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR),
                SeqCenter.findByNameAndDirName(SEQ_CENTER ,SEQ_CENTER_DIR)
        )
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterTwiceAndSeqCenterDir() {
        seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR)
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(SEQ_CENTER, DIFFERENT_SEQ_CENTER_DIR)
        }
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterAndSeqCenterDirTwice() {
        seqCenterService.createSeqCenter(SEQ_CENTER, SEQ_CENTER_DIR)
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(DIFFERENT_SEQ_CENTER, SEQ_CENTER_DIR)
        }
    }

    @Test
    void testCreateSeqCenterUsingNullAndSeqCenterDir() {
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(null, SEQ_CENTER_DIR)
        }
    }

    @Test
    void testCreateSeqCenterUsingSeqCenterAndNull() {
        shouldFail(AssertionError) {
            seqCenterService.createSeqCenter(SEQ_CENTER, null)
        }
    }
}

