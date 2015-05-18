package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.TestFor
import org.junit.Test

/**
 */
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
        SeqType seqType = new SeqType(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                dirName: "whole_genome_sequencing"
        )
        assert seqType.save(flush: true)

        shouldFail(AssertionError) {
            SeqType.wholeGenomePairedSeqType
        }
    }

    @Test
    void testGetWholeGenomePairedSeqType_AllFine() {
        SeqType seqType = new SeqType(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName: "whole_genome_sequencing"
        )
        assert seqType.save(flush: true)

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
        SeqType seqType = new SeqType(
                name: SeqTypeNames.EXOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                dirName: "exome_sequencing"
        )
        assert seqType.save(flush: true)
        shouldFail(AssertionError) {
            SeqType.exomePairedSeqType
        }
    }

    @Test
    void testGetExomePairedSeqType_AllFine() {
        SeqType seqType = new SeqType(
                name: SeqTypeNames.EXOME.seqTypeName,
                alias: SeqTypeNames.EXOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName: "exome_sequencing"
        )
        assert seqType.save(flush: true)
        assert seqType == SeqType.exomePairedSeqType
    }

}
