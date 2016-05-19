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

    @Test
    void testFindOutMateNumber_RegExprMatches() {
        def files = [
            [name: 'SOMEPID_L001_R2.fastq.gz', mateNumber: 2],
            [name: 's_101202_7_1.fastq.gz', mateNumber: 1],
            [name: 's_110421_3.read2.fastq.gz', mateNumber: 2],
            [name: 's_3_1_sequence.txt.gz', mateNumber: 1],
            [name: 's_140304_3_001_2_sequence.txt.gz', mateNumber: 2],
            [name: 's_111201_2a_1_sequence.txt.gz', mateNumber: 1],
            [name: 'SOMEPID_s_6_2_sequence.txt.gz', mateNumber: 2],
            [name: 'SOMEPID_s_3_1_sequence.txt.gz', mateNumber: 1],
            [name: 'AB-1234_CDE_EFGH_091_lib14837_1189_7_1.fastq.tar.bz', mateNumber: 1],
            [name: 'AB-1234_CDE_EFGH_091_lib114837_1189_7_1.fastq.tar.bz', mateNumber: 1],
            [name: 'AB-1234_5647_lib12345_1_sequence.fastq.bz2', mateNumber: 1],
            [name: 'CD-2345_6789_lib234567_7890_1.fastq.bz2', mateNumber: 1],
            [name: 'NB_E_789_R.2.fastq.gz', mateNumber: 2],
            [name: 'NB_E_234_R5.2.fastq.gz', mateNumber: 2],
            [name: 'NB_E_345_T1S.2.fastq.gz', mateNumber: 2],
            [name: 'NB_E_456_O_lane5.2.fastq.gz', mateNumber: 2],
            [name: '00_MCF10A_GHI_JKL_WGBS_I.A34002.137487.C2RT2ACXX.1.1.fastq.gz', mateNumber: 1],
            [name: '00_MCF10A_GHI_JKL_H3K4me1_I.IX1239-A26685-ACTTGA.134224.D2B0LACXX.2.1.fastq.gz', mateNumber: 1],
            [name: 'RB7_Blut_R1.fastq.gz', mateNumber: 1],
            [name: 'P021_WXYZ_L1_Rep3_2.fastq.gz', mateNumber: 2],
            [name: 'H019_ASDF_L1_lib54321_1.fastq.gz', mateNumber: 1],
            [name: 'FE-0100_H021_WXYZ_L1_5_1.fastq.gz', mateNumber: 1],
            [name: 'lane6mp25PE2_2_sequence.txt.gz', mateNumber: 2],
            [name: 'lane211s003107_1_sequence.txt.gz', mateNumber: 1],
            [name: 'lane8wwmp44PE2_1_sequence.txt.gz', mateNumber: 1],
            [name: 'SOMEPID_lane511s003237_1_sequence.txt.gz', mateNumber: 1],
            [name: '180824_I234_ABCDEFGHIJK_L5_WHAIPI000042-43_2.raw.fq.gz', mateNumber: 2],
            [name: 'FOOBAR_ATRT999_lib424242_1.fastq.gz', mateNumber: 1],
            [name: 'FOOBAR_ATRT999_lib424242_2.fastq.gz', mateNumber: 2],
            [name: 'AS-78217-LR-10215_R1.fastq.gz', mateNumber: 1],
            [name: 'AS-78217-LR-10219_R2.fastq.gz', mateNumber: 2],
            [name: 'SOMEPID_control_0097062_1.fastq.gz', mateNumber: 1],
            [name: 'SOMEPID_control_0097062_2.fastq.gz', mateNumber: 2],
            [name: 'EGAR00001234567_ABCDE_RNAseq_1.fq.gz', mateNumber: 1],
            [name: 'EGAR00001234567_ABCDE_RNAseq_2.fq.gz', mateNumber: 2],
        ]
        files.each { file ->
            assertEquals(file.mateNumber, MetaDataService.findOutMateNumber(file.name))
        }
    }

    @Test
    void testFindOutMateNumber_RegExprNotMatches() {
        def files = [
            'D0DDVABXX_lane4.fastq.gz',
        ]
        files.each { file ->
            assert shouldFail(RuntimeException, { MetaDataService.findOutMateNumber(file) }) ==~ /cannot\sfind.*/
        }
    }

    @Test
    void testFindOutMateNumber_InvalidInput() {
        assert shouldFail(AssertionError, { MetaDataService.findOutMateNumber(null) }) ==~ /.*file\sname\smust\sbe\sprovided.*/
    }
}
