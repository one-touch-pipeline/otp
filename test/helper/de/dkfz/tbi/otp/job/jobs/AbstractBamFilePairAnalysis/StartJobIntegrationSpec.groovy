package de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.OtrsTicket

trait StartJobIntegrationSpec {

    SamplePair setupSamplePair() {
        def map = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair = map.samplePair

        createConfig(samplePair, createPipeline())

        return samplePair
    }

    abstract Pipeline createPipeline()

    abstract BamFilePairAnalysis getInstance()

    abstract Date getStartedDate(OtrsTicket otrsTicket)

    abstract SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair)

    abstract ConfigPerProjectAndSeqType createConfig(SamplePair samplePair, Pipeline pipeline)

    abstract AbstractBamFilePairAnalysisStartJob getService()

    /**
     * Callback to set the processing status of our dependant for the test-samplepair.
     *
     * Each workflow startjob that depends on another workflow being in some state MUST
     * provide this method, so that tests in this abstract class can create the correct
     * dependency environment.
     */
    void setDependencyProcessingStatus(SamplePair samplePair, SamplePair.ProcessingStatus dependeeProcessingStatus) {
        throw new UnsupportedOperationException('Not supported for this test')
    }

    void createDependeeInstance(SamplePair samplePair, AnalysisProcessingStates dependeeAnalysisProcessingState) {
        throw new UnsupportedOperationException('Not supported for this test')
    }
}
