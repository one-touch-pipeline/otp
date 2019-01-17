package workflows.analysis.pair.bamfiles

trait SeqTypeAndInputBamFilesHCC1187Div8 implements SeqTypeAndInputBamFilesHCC1187Div {

    @Override
    BamFileSet getBamFileSet() {
        return getBamFileSet(8)
    }
}
