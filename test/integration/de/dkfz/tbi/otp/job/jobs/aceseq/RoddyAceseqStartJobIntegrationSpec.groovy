package de.dkfz.tbi.otp.job.jobs.aceseq

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.RoddyJobSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.WithReferenceGenomeRestrictionSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.OtrsTicket

class RoddyAceseqStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec implements RoddyJobSpec, WithReferenceGenomeRestrictionSpec {

    @Autowired
    RoddyAceseqStartJob roddyAceseqStartJob

    @Override
    Pipeline createPipeline() {
        DomainFactory.createAceseqPipelineLazy()
    }

    @Override
    AbstractBamFilePairAnalysisStartJob getService() {
        return roddyAceseqStartJob
    }

    @Override
    BamFilePairAnalysis getInstance() {
        return DomainFactory.createAceseqInstanceWithRoddyBamFiles()
    }

    @Override
    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.aceseqStarted
    }

    @Override
    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.aceseqProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = WithReferenceGenomeRestrictionSpec.super.setupSamplePair()

        // prepare a "finished" sophia analysis, since we depend on that.
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)
        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)

        return samplePair
    }

    @Override
    void setDependencyProcessingStatus(SamplePair samplePair, SamplePair.ProcessingStatus dependeeProcessingStatus) {
        samplePair.sophiaProcessingStatus = dependeeProcessingStatus
    }

    @Override
    void createDependeeInstance(SamplePair samplePair, AnalysisProcessingStates dependeeAnalysisProcessingState) {
        DomainFactory.createSophiaInstance(samplePair, [processingState: dependeeAnalysisProcessingState])
    }

    @Override
    ProcessingOption.OptionName getProcessingOptionNameForReferenceGenome() {
        return ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
    }
}
