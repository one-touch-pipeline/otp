package workflows.analysis.pair.snv

import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class RoddyBamFileOtpSnvWorkflowTests extends AbstractOtpSnvWorkflowTests implements SeqTypeAndInputWgsBamFiles {

    @Before
    void prepare() {
        setupRoddyBamFile()
    }
}
