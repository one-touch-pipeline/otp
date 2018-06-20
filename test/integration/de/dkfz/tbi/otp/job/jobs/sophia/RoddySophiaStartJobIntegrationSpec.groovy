package de.dkfz.tbi.otp.job.jobs.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*
import de.dkfz.tbi.otp.job.jobs.*

class RoddySophiaStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec {

    @Autowired
    RoddySophiaStartJob roddySophiaStartJob

    Pipeline createPipeline() {
        DomainFactory.createSophiaPipelineLazy()
    }

    AbstractBamFilePairAnalysisStartJob getService() {
        return roddySophiaStartJob
    }

    BamFilePairAnalysis getInstance() {
        return DomainFactory.createSophiaInstanceWithRoddyBamFiles()
    }

    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.sophiaStarted
    }

    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.sophiaProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = super.setupSamplePair()
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])
        return samplePair
    }
}
