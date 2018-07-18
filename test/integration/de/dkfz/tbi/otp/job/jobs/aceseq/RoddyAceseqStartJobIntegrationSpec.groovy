package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*

class RoddyAceseqStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec {

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
        SamplePair samplePair = super.setupSamplePair()

        // prepare a "finished" sophia analysis, since we depend on that.
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)
        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)

        // use whatever reference genome the tests auto-generated as the correct ones.
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])

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
}
