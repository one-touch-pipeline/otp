package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.joda.time.*
import org.joda.time.format.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component("roddySnvStartJob")
@Scope("singleton")
class RoddySnvCallingStartJob extends AbstractSnvCallingStartJob {

    @Override
    String getJobExecutionPlanName() {
        return "RoddySnvWorkflow"
    }

    @Override
    protected ConfigPerProject getConfig(SamplePair samplePair) {
        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByName(Pipeline.Name.RODDY_SNV))
        RoddyWorkflowConfig config = (RoddyWorkflowConfig)RoddyWorkflowConfig.getLatestForIndividual(
                samplePair.individual, samplePair.seqType, pipeline)

        if (config == null) {
            throw new RuntimeException("No ${RoddyWorkflowConfig.simpleName} found for ${Pipeline.simpleName} ${pipeline}, ${Individual.simpleName} ${samplePair.individual} (${Project.simpleName} ${samplePair.project}), ${SeqType.simpleName} ${samplePair.seqType}")
        }
        return config
    }

    @Override
    protected Class<? extends ConfigPerProject> getConfigClass() {
        return RoddyWorkflowConfig
    }

    @Override
    protected Class<? extends SnvCallingInstance> getInstanceClass() {
        return RoddySnvCallingInstance
    }
}
