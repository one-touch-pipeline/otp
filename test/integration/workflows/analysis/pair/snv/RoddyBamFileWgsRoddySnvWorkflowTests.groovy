package workflows.analysis.pair.snv

import org.junit.*

@Ignore
class RoddyBamFileWgsRoddySnvWorkflowTests extends AbstractRoddySnvWorkflowTests {

    @Before
    void prepare() {
        setupRoddyWgsBamFile()
    }
}
