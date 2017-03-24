package workflows.analysis.pair.snv

import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class ProcessedMergedBamFileOtpSnvWorkflowTests extends AbstractOtpSnvWorkflowTests implements SeqTypeAndInputWgsBamFiles {

    @Before
    void prepare() {
        setupProcessMergedBamFile()
    }
}
