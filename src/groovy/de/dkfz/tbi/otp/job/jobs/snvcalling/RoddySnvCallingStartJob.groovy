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
        return RoddyWorkflowConfig.getLatestForIndividual(samplePair.individual, samplePair.seqType,
                CollectionUtils.exactlyOneElement(Pipeline.findAllByName(Pipeline.Name.RODDY_SNV)))
    }

    @Override
    protected String getInstanceName(ConfigPerProject config) {
        String date = DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm_Z").withZone(ConfigService.getDateTimeZone()).print(Instant.now())
        return "results_${config.pluginVersion.replaceAll(":", "-")}_${config.configVersion}_${date}"
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
