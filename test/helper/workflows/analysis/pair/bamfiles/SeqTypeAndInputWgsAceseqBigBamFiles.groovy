package workflows.analysis.pair.bamfiles

trait SeqTypeAndInputWgsAceseqBigBamFiles implements SeqTypeAndInputBamFiles {

    @Override
    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), 'aceseq'),
                'HCC1187C_S1.bam',
                'HCC1187C_S1.bam.bai',
                'HCC1187BL_S1.bam',
                'HCC1187BL_S1.bam.bai '
        )
    }
}
