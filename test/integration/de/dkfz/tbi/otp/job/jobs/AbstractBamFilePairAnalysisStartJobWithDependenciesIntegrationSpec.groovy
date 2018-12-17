package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair

abstract class AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec {

    void "don't findSamplePairToProcess when prereq not yet done"() {
        given:
        SamplePair samplePair = setupSamplePair()
        setDependencyProcessingStatus(samplePair, SamplePair.ProcessingStatus.NEEDS_PROCESSING)
        samplePair.save(flush: true)

        expect:
        null == getService().findSamplePairToProcess(ProcessingPriority.NORMAL)
    }

    void "don't findSamplePairToProcess, when at least one prereq still running"() {
        given:
        SamplePair samplePair = setupSamplePair()
        setDependencyProcessingStatus(samplePair, SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED)
        samplePair.save(flush: true)

        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)
        createDependeeInstance(samplePair, AnalysisProcessingStates.IN_PROGRESS)

        expect:
        null == getService().findSamplePairToProcess(ProcessingPriority.NORMAL)
    }
}
