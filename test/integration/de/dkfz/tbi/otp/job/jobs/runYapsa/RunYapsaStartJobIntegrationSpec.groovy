package de.dkfz.tbi.otp.job.jobs.runYapsa

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.WithReferenceGenomeRestrictionSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.OtrsTicket

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
