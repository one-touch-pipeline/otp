package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

public class RoddySnvCallingStartJobTests {

    private static long ARBITRARY_TIMESTAMP = 1337

    @Autowired
    private RoddySnvCallingStartJob roddySnvCallingStartJob

    SnvCallingInstanceTestData snvTestData

    @Test
    void test_ConfigClass_Config_Instance() {
        snvTestData = new SnvCallingInstanceTestData()
        snvTestData.createSnvObjects()
        DomainFactory.createProcessingOption(name: 'timeZone', type: null, value: 'Europe/Berlin')

        try {
            SamplePair samplePair = snvTestData.samplePair
            Project project = samplePair.project
            SeqType seqType = samplePair.seqType
            RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig(project: project, seqType: seqType, pipeline: DomainFactory.createRoddySnvPipelineLazy(), pluginVersion: 'pluginVersion:1.1.0', configVersion: 'v1_0')
            DateTimeUtils.setCurrentMillisFixed(ARBITRARY_TIMESTAMP)

            assert roddySnvCallingStartJob.getConfigClass() == RoddyWorkflowConfig
            assert roddySnvCallingStartJob.getConfig(samplePair) == config
            assert roddySnvCallingStartJob.getInstanceName(config) == "results_pluginVersion-1.1.0_v1_0_1970-01-01_01h00_+0100".toString()
        }  finally {
            DateTimeUtils.setCurrentMillisSystem()
        }
    }
}
