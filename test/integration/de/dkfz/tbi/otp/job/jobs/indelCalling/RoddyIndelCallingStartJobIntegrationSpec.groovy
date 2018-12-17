package de.dkfz.tbi.otp.job.jobs.indelCalling

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.RoddyJobSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysisStartJobIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.OtrsTicket

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
