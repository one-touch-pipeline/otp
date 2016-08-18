package workflows.snv

import org.junit.*

@Ignore
class ProcessedMergedBamFileOtpSnvWorkflowTests extends AbstractOtpSnvWorkflowTests {

    @Before
    void prepare() {
        setupProcessMergedBamFile()
    }
}
