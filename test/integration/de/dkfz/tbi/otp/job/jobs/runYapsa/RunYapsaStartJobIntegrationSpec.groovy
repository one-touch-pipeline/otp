package de.dkfz.tbi.otp.job.jobs.runYapsa

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.springframework.beans.factory.annotation.*

class RunYapsaStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec {

    @Autowired
    RunYapsaStartJob runYapsaStartJob

    @Override
    Pipeline createPipeline() {
        DomainFactory.createRunYapsaPipelineLazy()
    }

    @Override
    AbstractBamFilePairAnalysisStartJob getService() {
        return runYapsaStartJob
    }

    @Override
    BamFilePairAnalysis getInstance() {
        return DomainFactory.createRunYapsaInstanceWithRoddyBamFiles()
    }

    @Override
    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.runYapsaStarted
    }

    @Override
    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.runYapsaProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = super.setupSamplePair()

        // at time of writing, runYapsa supports only WES+WGS
        // Make sure they are available for configs
        DomainFactory.createExomeSeqType()
        DomainFactory.createWholeGenomeSeqType()

        // fake a "finished" SNV calling for us to analyse
        samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)

        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)

        // use whatever auto-generated reference genome the test generated as the only "usable" one.
        DomainFactory.createProcessingOptionLazy([
                name   : ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME,
                type   : null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])

        return samplePair
    }

    @Override
    void setDependencyProcessingStatus(SamplePair samplePair, SamplePair.ProcessingStatus dependeeProcessingStatus) {
        samplePair.snvProcessingStatus = dependeeProcessingStatus
    }

    @Override
    void createDependeeInstance(SamplePair samplePair, AnalysisProcessingStates dependeeAnalysisProcessingState) {
        DomainFactory.createRoddySnvCallingInstance(samplePair, [processingState: dependeeAnalysisProcessingState,])
    }
}
