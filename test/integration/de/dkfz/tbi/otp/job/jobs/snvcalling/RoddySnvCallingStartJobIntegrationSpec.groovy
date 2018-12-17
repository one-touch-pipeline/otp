package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.RoddyJobSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysisStartJobIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.OtrsTicket

class RoddySnvCallingStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec implements RoddyJobSpec {

    @Autowired
    RoddySnvCallingStartJob roddySnvCallingStartJob

    void setup() {
        DomainFactory.createSnvSeqTypes()
    }

    @Override
    Pipeline createPipeline() {
        DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    AbstractBamFilePairAnalysisStartJob getService() {
        return roddySnvCallingStartJob
    }

    @Override
    BamFilePairAnalysis getInstance() {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
    }

    @Override
    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.snvStarted
    }

    @Override
    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.snvProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = DomainFactory.createSamplePairWithProcessedMergedBamFiles()

        DomainFactory.createRoddyWorkflowConfig(
                project: samplePair.project,
                seqType: samplePair.seqType,
                pipeline: DomainFactory.createRoddySnvPipelineLazy(),
        )

        return samplePair
    }
}
