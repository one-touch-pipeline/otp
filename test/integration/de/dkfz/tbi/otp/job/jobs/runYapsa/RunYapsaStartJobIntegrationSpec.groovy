package de.dkfz.tbi.otp.job.jobs.runYapsa

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.*

class RunYapsaStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec implements WithReferenceGenomeRestrictionSpec {

    @Autowired
    RunYapsaStartJob runYapsaStartJob

    void setup() {
        DomainFactory.createExomeSeqType()
    }

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
        SamplePair samplePair = WithReferenceGenomeRestrictionSpec.super.setupSamplePair()

        // fake a "finished" SNV calling for us to analyse
        samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)

        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)

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

    @Override
    ConfigPerProjectAndSeqType createConfig(SamplePair samplePair, Pipeline pipeline) {
        return DomainFactory.createRunYapsaConfig(
                [
                        project : samplePair.project,
                        seqType : samplePair.seqType,
                        pipeline: pipeline,
                ]
        )
    }

    @Override
    ProcessingOption.OptionName getProcessingOptionNameForReferenceGenome() {
        return ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME
    }
}
