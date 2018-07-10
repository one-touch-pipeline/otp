package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*

abstract class AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec {

    void "don't findSamplePairToProcess when prereq not yet done"() {
        given:
        SamplePair samplePair = setupSamplePair()
        setDependencyProcessingStatus(samplePair, SamplePair.ProcessingStatus.NEEDS_PROCESSING)
        samplePair.save(flush: true)

        expect:
        null == getService().findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "don't findSamplePairToProcess, when at least one prereq still running"() {
        given:
        SamplePair samplePair = setupSamplePair()
        setDependencyProcessingStatus(samplePair, SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED)
        samplePair.save(flush: true)

        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)
        createDependeeInstance(samplePair, AnalysisProcessingStates.IN_PROGRESS)

        expect:
        null == getService().findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }

    /**
     * Callback to set the processing status of our dependant for the test-samplepair.
     *
     * Each workflow startjob that depends on another workflow being in some state MUST
     * provide this method, so that tests in this abstract class can create the correct
     * dependency environment.
     */
    abstract void setDependencyProcessingStatus(SamplePair samplePair, SamplePair.ProcessingStatus dependeeProcessingStatus)

    abstract void createDependeeInstance(SamplePair samplePair, AnalysisProcessingStates dependeeAnalysisProcessingState)
}
