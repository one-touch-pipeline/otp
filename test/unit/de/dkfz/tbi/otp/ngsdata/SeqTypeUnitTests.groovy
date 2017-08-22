package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import grails.test.mixin.*
import org.junit.*

@TestFor(SeqType)
class SeqTypeUnitTests {

    @Test
    void testGetWholeGenomePairedSeqType_NoSeqTypeInDB_ShouldFail() {
        shouldFail(AssertionError) {
            SeqType.wholeGenomePairedSeqType
        }
    }

    @Test
    void testGetWholeGenomePairedSeqType_NoWGSPairedSeqTypeInDB_ShouldFail() {
        DomainFactory.createWholeGenomeSeqType(SeqType.LIBRARYLAYOUT_SINGLE)

        shouldFail(AssertionError) {
            SeqType.wholeGenomePairedSeqType
        }
    }

    @Test
    void testGetWholeGenomePairedSeqType_AllFine() {
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        assert seqType == SeqType.wholeGenomePairedSeqType
    }


    @Test
    void testGetExomePairedSeqType_NoSeqTypeInDB_ShouldFail() {
        shouldFail(AssertionError) {
            SeqType.exomePairedSeqType
        }
    }

    @Test
    void testGetExomePairedSeqType_NoExomePairedSeqTypeInDB_ShouldFail() {
        DomainFactory.createExomeSeqType(SeqType.LIBRARYLAYOUT_SINGLE)
        shouldFail(AssertionError) {
            SeqType.exomePairedSeqType
        }
    }

    @Test
    void testGetExomePairedSeqType_AllFine() {
        SeqType seqType = DomainFactory.createExomeSeqType()
        assert seqType.save(flush: true)
        assert seqType == SeqType.exomePairedSeqType
    }

    @Test
    void testCreateSeqTypesWithUniqueNameAndLibraryLayoutCombination_AllFine() {
        assert DomainFactory.createSeqType(
                name: "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        )
        assert DomainFactory.createSeqType(
                name: "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
        )
        assert DomainFactory.createSeqType(
                name: "seqTypeName2",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        )
    }

    @Test
    void testCreateSeqTypesWithNonUniqueNameAndLibraryLayoutCombination_ShouldFail() {
        assert DomainFactory.createSeqType(
                name: "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        )
        SeqType seqType = DomainFactory.createSeqType([
                    name: "seqTypeName1",
                    libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
            ], false)
        TestCase.assertValidateError(seqType, "libraryLayout", "unique", LibraryLayout.PAIRED.name())
    }
}
