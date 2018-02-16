package workflows.analysis.pair.snv

import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class ExternalBamFileWgsRoddySnvWorkflowTests extends AbstractRoddySnvWorkflowTests implements SeqTypeAndInputWgsBamFiles {

    @Before
    void prepare() {
        setupExternalBamFile()
    }
}
