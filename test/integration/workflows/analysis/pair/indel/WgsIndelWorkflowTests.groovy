package workflows.analysis.pair.indel

import org.junit.*

@Ignore
class WgsIndelWorkflowTests extends AbstractIndelWorkflowTests {

    @Test
    void testWholeWorkflowWithRoddyBamFile() {
        setupRoddyWgsBamFile()
        createConfig()

        execute()
        check()
    }

    @Test
    void testWholeWorkflowWithProcessedMergedBamFile() {
        setupProcessMergedWgsBamFile()
        createConfig()

        execute()
        check()
    }
}
