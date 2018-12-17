package de.dkfz.tbi.otp.job.jobs.sophia

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.RoddyJobSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.WithReferenceGenomeRestrictionSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysisStartJobIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.OtrsTicket

class RoddySophiaStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec implements RoddyJobSpec, WithReferenceGenomeRestrictionSpec {

    @Autowired
    RoddySophiaStartJob roddySophiaStartJob

    @Override
    Pipeline createPipeline() {
        DomainFactory.createSophiaPipelineLazy()
    }

    @Override
    AbstractBamFilePairAnalysisStartJob getService() {
        return roddySophiaStartJob
    }

    @Override
    BamFilePairAnalysis getInstance() {
        return DomainFactory.createSophiaInstanceWithRoddyBamFiles()
    }

    @Override
    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.sophiaStarted
    }

    @Override
    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.sophiaProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = WithReferenceGenomeRestrictionSpec.super.setupSamplePair()
        return samplePair
    }

    @Override
    ProcessingOption.OptionName getProcessingOptionNameForReferenceGenome() {
        return ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
    }
}
