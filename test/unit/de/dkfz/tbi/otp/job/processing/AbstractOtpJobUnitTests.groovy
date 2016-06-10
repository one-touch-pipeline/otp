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
        String expected = "Log file: ${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}"

        assert AbstractOtpJob.getLogFileNames(clusterJob) == expected
    }
}
