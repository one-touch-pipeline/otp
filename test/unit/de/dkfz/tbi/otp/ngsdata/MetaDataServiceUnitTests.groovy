package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.ngsdata.MetaDataService.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(MetaDataService)
class MetaDataServiceUnitTests {

    @Test
    void testEnsurePairedSequenceFileNameConsistency_okay() {
        ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc2abc.fastq')
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_differentLengths() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc2abcd.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_same() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc1abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_incorrectOrder() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc2abc.fastq', 'abc1abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_illegalMateNumber1() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc0abc.fastq', 'abc2abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_illegalMateNumber2() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc.fastq', 'abc3abc.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_tooManyDifferences() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc1abc1.fastq', 'abc2abc2.fastq') }
    }

    @Test
    void testEnsurePairedSequenceFileNameConsistency_tooLongDifference() {
        shouldFail { ensurePairedSequenceFileNameConsistency('abc11abc.fastq', 'abc22abc.fastq') }
    }
}
