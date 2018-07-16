package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*

class RoddyIndelCallingStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec implements RoddyJobSpec {

    @Autowired
    RoddyIndelCallingStartJob roddyIndelCallingStartJob

    void setup() {
        DomainFactory.createIndelSeqTypes()
    }

    @Override
    Pipeline createPipeline() {
        DomainFactory.createIndelPipelineLazy()
    }

    @Override
    AbstractBamFilePairAnalysisStartJob getService() {
        return roddyIndelCallingStartJob
    }

    @Override
    BamFilePairAnalysis getInstance() {
        return DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
    }

    @Override
    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.indelStarted
    }

    @Override
    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.indelProcessingStatus
    }
}
