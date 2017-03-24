package workflows.analysis.pair.snv

import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class RoddyBamFileWesRoddySnvWorkflowTests extends AbstractRoddySnvWorkflowTests implements SeqTypeAndInputWesBamFiles {

    @Before
    void prepare() {
        setupRoddyBamFile()
    }
}
