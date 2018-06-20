package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import grails.test.spock.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.jobs.*

class RoddyIndelCallingStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec {

    @Autowired
    RoddyIndelCallingStartJob roddyIndelCallingStartJob

    void setup() {
        DomainFactory.createIndelSeqTypes()
    }

    Pipeline createPipeline() {
        DomainFactory.createIndelPipelineLazy()
    }

    AbstractBamFilePairAnalysisStartJob getService() {
        return roddyIndelCallingStartJob
    }

    BamFilePairAnalysis getInstance() {
        return DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
    }

    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.indelStarted
    }

    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.indelProcessingStatus
    }
}
