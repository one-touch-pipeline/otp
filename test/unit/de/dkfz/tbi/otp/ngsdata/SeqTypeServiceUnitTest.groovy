package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*


@Build([SeqType])
class SeqTypeServiceUnitTest {

    SeqTypeService seqTypeService

    void setUp() {
        seqTypeService = new SeqTypeService()
    }



     void testAlignableSeqTypes() {
        DomainFactory.createAlignableSeqTypes()

        List<SeqType> alignableSeqTypes = SeqTypeService.alignableSeqTypes()
        assert 2 == alignableSeqTypes.size()
        assert alignableSeqTypes.find{ it.name == SeqTypeNames.EXOME.seqTypeName && it.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED}
        assert alignableSeqTypes.find{ it.name == SeqTypeNames.WHOLE_GENOME.seqTypeName && it.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED}
    }
}
