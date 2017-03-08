package workflows.analysis.pair.indel

import org.junit.*

@Ignore
class WesIndelWorkflowTests extends AbstractIndelWorkflowTests {

    @Test
    void testWholeWorkflowWithRoddyBamFile() {
        setupRoddyWesBamFile()
        createConfig()

        execute()
        check()
    }

    @Test
    void testWholeWorkflowWithProcessedMergedBamFile() {
        setupProcessMergedWesBamFile()
        createConfig()

        execute()
        check()
    }
}
