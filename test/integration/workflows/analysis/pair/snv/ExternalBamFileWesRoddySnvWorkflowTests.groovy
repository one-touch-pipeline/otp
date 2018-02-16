package workflows.analysis.pair.snv

import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class ExternalBamFileWesRoddySnvWorkflowTests extends AbstractRoddySnvWorkflowTests implements SeqTypeAndInputWesBamFiles {

    @Before
    void prepare() {
        setupExternalBamFile()
    }
}
