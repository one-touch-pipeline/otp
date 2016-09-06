package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import org.joda.time.format.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component("snvStartJob")
@Scope("singleton")
class  SnvCallingStartJob extends AbstractSnvCallingStartJob {

    @Override
    String getJobExecutionPlanName() {
        return "SnvWorkflow"
    }

    @Override
    protected ConfigPerProject getConfig(SamplePair samplePair) {
        return SnvConfig.getLatest(
                samplePair.project,
                samplePair.seqType
        )
    }

    @Override
    protected String getInstanceName(ConfigPerProject config) {
        return DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm_Z").withZone(ConfigService.getDateTimeZone()).print(Instant.now())
    }

    @Override
    protected Class<? extends ConfigPerProject> getConfigClass() {
        return SnvConfig
    }

    @Override
    protected Class<? extends SnvCallingInstance> getInstanceClass() {
        return SnvCallingInstance
    }
}
