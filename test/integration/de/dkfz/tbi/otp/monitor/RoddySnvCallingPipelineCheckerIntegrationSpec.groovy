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


    protected void createExpectedCall(String workFlowName, MonitorOutputCollector output, BamFilePairAnalysis runningAnalysis) {
        1 * output.showRunning('SnvWorkflow', null)
        1 * output.showRunning('RoddySnvWorkflow', [runningAnalysis])
        0 * output.showRunning(_, _)
    }
}
