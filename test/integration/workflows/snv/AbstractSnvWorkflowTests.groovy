package workflows.snv

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import org.junit.*
import workflows.analysis.pair.AbstractBamFilePairAnalysisWorkflowTests

abstract class AbstractSnvWorkflowTests extends AbstractBamFilePairAnalysisWorkflowTests {


    @Test
    void testWholeSnvWorkflow() {
        createConfig()

        execute()
        check()
    }


    final void check() {
        checkInstanceFinished()
        checkSpecific()
    }


    abstract void checkSpecific()


    void checkInstanceFinished() {
        SnvCallingInstance createdInstance = SnvCallingInstance.listOrderById().last()
        assert createdInstance.processingState == AnalysisProcessingStates.FINISHED
        assert createdInstance.config == config
        assert createdInstance.sampleType1BamFile == bamFileTumor
        assert createdInstance.sampleType2BamFile == bamFileControl
    }


    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'snv')
    }
}
