package workflows.analysis.pair.bamfiles

trait SeqTypeAndInputWgsAceseqBigBamFiles implements SeqTypeAndInputBamFiles {

    @Override
    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(new File(getBamFilePairBaseDirectory(), 'wgs'), 'aceseq'),
                'tumor_HCC1187_merged.mdup.bam',
                'tumor_HCC1187_merged.mdup.bam.bai',
                'blood_HCC1187_merged.mdup.bam',
                'blood_HCC1187_merged.mdup.bam.bai '
        )
    }
}
