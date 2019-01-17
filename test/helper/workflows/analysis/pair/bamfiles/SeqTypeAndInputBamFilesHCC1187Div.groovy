package workflows.analysis.pair.bamfiles

trait SeqTypeAndInputBamFilesHCC1187Div implements SeqTypeAndInputBamFiles {

    @SuppressWarnings('JavaIoPackageAccess')
    BamFileSet getBamFileSet(int div) {
        return new BamFileSet(
                new File(getBamFilePairBaseDirectory(), "small/div${div}"),
                "tumor_HCC1187-div${div}_merged.mdup.bam",
                "tumor_HCC1187-div${div}_merged.mdup.bam.bai",
                "blood_HCC1187-div${div}_merged.mdup.bam",
                "blood_HCC1187-div${div}_merged.mdup.bam.bai "
        )
    }
}
