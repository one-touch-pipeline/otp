package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.job.jobs.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.TIME_ZONE

class RoddySnvCallingStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec {

    @Autowired
    RoddySnvCallingStartJob roddySnvCallingStartJob

    void setup() {
        DomainFactory.createSnvSeqTypes()
    }

    Pipeline createPipeline() {
        DomainFactory.createRoddySnvPipelineLazy()
    }

    AbstractBamFilePairAnalysisStartJob getService() {
        return roddySnvCallingStartJob
    }

    BamFilePairAnalysis getInstance() {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
    }

    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.snvStarted
    }

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
