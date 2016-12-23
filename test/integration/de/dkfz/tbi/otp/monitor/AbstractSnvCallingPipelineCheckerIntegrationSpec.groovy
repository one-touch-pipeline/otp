package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

abstract class AbstractSnvCallingPipelineCheckerIntegrationSpec extends AbstractVariantCallingPipelineCheckerIntegrationSpec {

    @Override
    AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker() {
        return new SnvCallingPipelineChecker()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createIndelPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysisForCrosschecking(Map properties) {
        return DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(properties)
    }

}
