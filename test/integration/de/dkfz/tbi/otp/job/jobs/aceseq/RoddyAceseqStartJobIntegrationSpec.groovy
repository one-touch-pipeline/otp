package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*
import de.dkfz.tbi.otp.job.jobs.*

class RoddyAceseqStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec {

    @Autowired
    RoddyAceseqStartJob roddyAceseqStartJob

    void "findSamplePairToProcess, Sophia not started"() {
        given:
        SamplePair samplePair = setupSamplePair()
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
        samplePair.save(flush: true)

        expect:
        null == roddyAceseqStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "findSamplePairToProcess, one Sophia finished and one running"() {
        given:
        SamplePair samplePair = setupSamplePair()
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)
        DomainFactory.createSophiaInstance(samplePair)
        SophiaInstance si = DomainFactory.createSophiaInstance(samplePair)
        si.processingState = AnalysisProcessingStates.IN_PROGRESS
        si.save(flush: true)

        expect:
        null == roddyAceseqStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }

    Pipeline createPipeline() {
        DomainFactory.createAceseqPipelineLazy()
    }

    AbstractBamFilePairAnalysisStartJob getService() {
        return roddyAceseqStartJob
    }

    BamFilePairAnalysis getInstance() {
        return DomainFactory.createAceseqInstanceWithRoddyBamFiles()
    }

    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.aceseqStarted
    }

    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.aceseqProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = super.setupSamplePair()
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)
        DomainFactory.createSophiaInstance(samplePair)
        return samplePair
    }
}
