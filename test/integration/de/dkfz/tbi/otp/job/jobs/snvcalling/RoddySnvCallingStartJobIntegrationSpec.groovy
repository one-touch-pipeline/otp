package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*

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
