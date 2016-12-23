package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddySnvCallingPipelineCheckerIntegrationSpec extends AbstractSnvCallingPipelineCheckerIntegrationSpec {

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(properties)
    }


    void "displayRunning, check that running is called with expected parameters"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        SamplePair samplePair = DomainFactory.createSamplePairPanCan([
                (pipelineChecker.getProcessingStateMember()): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        BamFilePairAnalysis runningAnalysis = createAnalysis([
                samplePair: samplePair,
        ])

        when:
        pipelineChecker.displayRunning([runningAnalysis], output)

        then:
        1 * output.showRunning('SnvWorkflow', null)
        1 * output.showRunning('RoddySnvWorkflow', [runningAnalysis])
    }
}
