package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

public class SnvCallingStartJobTests {

    private static long ARBITRARY_TIMESTAMP = 1337

    @Autowired
    private SnvCallingStartJob snvCallingStartJob

    SnvCallingInstanceTestData snvTestData

    @Test
    void test_ConfigClass_Config_Instance() {
        snvTestData = new SnvCallingInstanceTestData()
        snvTestData.createSnvObjects()
        DomainFactory.createProcessingOption(name: 'TIME_ZONE', type: null, value: 'Europe/Berlin')

        try {
            SamplePair samplePair = snvTestData.samplePair
            SnvConfig config = snvTestData.createSnvConfig()
            DateTimeUtils.setCurrentMillisFixed(ARBITRARY_TIMESTAMP)

            assert snvCallingStartJob.getConfigClass() == SnvConfig
            assert snvCallingStartJob.getConfig(samplePair) == config
            assert snvCallingStartJob.getInstanceName(config) == '1970-01-01_01h00_+0100'
        }  finally {
            DateTimeUtils.setCurrentMillisSystem()
        }
    }
}
