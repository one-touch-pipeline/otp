package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.After
import org.junit.Before
import org.junit.Test

@TestFor(SeqTypeService)
@Build([SeqType])
class SeqTypeServiceUnitTests {

    SeqTypeService seqTypeService

    final static String SEQ_TYPE ="SeqType"

    final static String SEQ_TYPE_DIR ="SeqTypeDir"

    final static String SEQ_TYPE_ALIAS ="SeqTypeAlias"

    @Before
    public void setUp() throws Exception {
        seqTypeService = new SeqTypeService()
    }


    @After
    public void tearDown() throws Exception {
        seqTypeService = null
    }

    @Test
    void testAlignableSeqTypes() {
        DomainFactory.createDefaultOtpAlignableSeqTypes()

        List<SeqType> alignableSeqTypes = SeqTypeService.alignableSeqTypes()
        assert 2 == alignableSeqTypes.size()
        assert alignableSeqTypes.find{ it.name == SeqTypeNames.EXOME.seqTypeName && it.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED}
        assert alignableSeqTypes.find{ it.name == SeqTypeNames.WHOLE_GENOME.seqTypeName && it.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED}
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndSeqTypeDirAndSeqTypeAliasAndSingle() {
        assertEquals(
                seqTypeService.createSeqType(SEQ_TYPE, SEQ_TYPE_DIR, SEQ_TYPE_ALIAS, SeqType.LIBRARYLAYOUT_SINGLE),
                SeqType.findByNameAndDirNameAndLibraryLayout(SEQ_TYPE, SEQ_TYPE_DIR,  SeqType.LIBRARYLAYOUT_SINGLE)
        )
    }

    @Test
    void testCreateSeqTypeUsingNullAndSeqTypeDirAndSeqTypeAliasAndSingle() {
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(null, SEQ_TYPE_DIR, SEQ_TYPE_ALIAS, SeqType.LIBRARYLAYOUT_SINGLE)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndNullAndSeqTypeAliasAndSingle() {
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(SEQ_TYPE, null, SEQ_TYPE_ALIAS, SeqType.LIBRARYLAYOUT_SINGLE)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndSeqTypeDirAndSeqTypeAliasAndNull() {
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(SEQ_TYPE, SEQ_TYPE_DIR, SEQ_TYPE_ALIAS, null)
        }
    }

    @Test
    void testCreateSeqTypeUsingSeqTypeAndSeqTypeDirAndSeqTypeAliasAndSingleTwice() {
        seqTypeService.createSeqType(SEQ_TYPE, SEQ_TYPE_DIR, SEQ_TYPE_ALIAS, SeqType.LIBRARYLAYOUT_SINGLE)
        shouldFail(AssertionError) {
            seqTypeService.createSeqType(SEQ_TYPE, SEQ_TYPE_DIR, SEQ_TYPE_ALIAS, SeqType.LIBRARYLAYOUT_SINGLE)
        }
    }
}
