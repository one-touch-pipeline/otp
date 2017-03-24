package workflows.analysis.pair

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

abstract class AbstractRoddyBamFilePairAnalysisWorkflowTests<Instance extends BamFilePairAnalysis> extends AbstractBamFilePairAnalysisWorkflowTests {

    @Test
    void testWholeWorkflowWithRoddyBamFile() {
        setupRoddyBamFile()

        executeTest()
    }

    @Test
    void testWholeWorkflowWithProcessedMergedBamFile() {
        setupProcessMergedBamFile()

        executeTest()
    }


    @Override
    ReferenceGenome createReferenceGenome() {
        return createAndSetup_Bwa06_1K_ReferenceGenome()
    }


    void executeTest() {
        createConfig()

        execute()

        checkInstance()
    }


    void checkInstance() {
        Instance createdInstance = BamFilePairAnalysis.listOrderById().last()
        assert createdInstance.processingState == AnalysisProcessingStates.FINISHED
        assert createdInstance.config == config
        assert createdInstance.sampleType1BamFile == bamFileTumor
        assert createdInstance.sampleType2BamFile == bamFileControl

        filesToCheck(createdInstance).flatten().each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }
        checkAnalysisSpecific(createdInstance)
    }

    abstract List<File> filesToCheck(Instance instance)

    void checkAnalysisSpecific(Instance instance) {}

}
