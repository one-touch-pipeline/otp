package workflows.analysis.pair.bamfiles

trait SeqTypeAndInputBigBamFiles implements SeqTypeAndInputBamFiles {

    BamFileSet getBamFileSet() {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), 'gms'),
                'tumor_run1_gerald_D1VCPACXX_1_paired.bam.sorted.bam',
                'tumor_run1_gerald_D1VCPACXX_1_paired.bam.sorted.bam.bai',
                'normal_run1_gerald_D1VCPACXX_6_paired.bam.sorted.bam',
                'normal_run1_gerald_D1VCPACXX_6_paired.bam.sorted.bam.bai '
        )
    }
}
