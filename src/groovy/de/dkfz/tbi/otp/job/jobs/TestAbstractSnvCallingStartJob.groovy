package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component("testAbstractSnvStartJob")
@Scope("prototype")
class TestAbstractSnvCallingStartJob extends AbstractBamFilePairAnalysisStartJob {

    @Override
    String getJobExecutionPlanName() {
        throw new UnsupportedOperationException()
    }

    @Override
    protected ConfigPerProject getConfig(SamplePair samplePair) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected String getInstanceName(ConfigPerProject config) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected Class<? extends ConfigPerProject> getConfigClass() {
        throw new UnsupportedOperationException()
    }

    @Override
    protected Class<? extends SnvCallingInstance> getInstanceClass() {
        throw new UnsupportedOperationException()
    }

    @Override
    protected SamplePair findSamplePairToProcess(short minPriority) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        throw new UnsupportedOperationException()
    }
}
