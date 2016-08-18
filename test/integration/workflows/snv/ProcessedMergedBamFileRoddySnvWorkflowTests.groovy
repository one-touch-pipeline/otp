package workflows.snv

import org.junit.*

@Ignore
class ProcessedMergedBamFileRoddySnvWorkflowTests extends AbstractRoddySnvWorkflowTests {

    @Before
    void prepare() {
        setupProcessMergedBamFile()
    }
}
