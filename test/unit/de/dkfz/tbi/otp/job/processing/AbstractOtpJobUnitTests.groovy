package de.dkfz.tbi.otp.job.processing

import org.junit.Test

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import grails.buildtestdata.mixin.Build

@Build([
        ClusterJob,
])
class AbstractOtpJobUnitTests {

    @Test
    void testGetLogFileNames() {
        ClusterJob clusterJob = ClusterJob.build()
        String expected = "Output log file: ${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}\n" +
                "Error log file: ${clusterJob.clusterJobName}.e${clusterJob.clusterJobId}"

        assert AbstractOtpJob.getLogFileNames(clusterJob) == expected
    }
}
