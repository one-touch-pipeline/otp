package workflows.analysis.pair.snv

import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class RoddyBamFileWgsRoddySnvWorkflowTests extends AbstractRoddySnvWorkflowTests implements SeqTypeAndInputWgsBamFiles {

    @Before
    void prepare() {
        setupRoddyBamFile()
    }
}
