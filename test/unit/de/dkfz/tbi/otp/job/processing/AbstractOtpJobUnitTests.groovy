package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.test.mixin.Mock
import org.junit.Test

import de.dkfz.tbi.otp.infrastructure.ClusterJob


@Mock([
        ClusterJob,
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingStep,
        ProcessingStepUpdate,
        Realm,
])
class AbstractOtpJobUnitTests {

    @Test
    void testGetLogFileNames() {
        ClusterJob clusterJob = DomainFactory.createClusterJob()
        DomainFactory.createProcessingStepUpdate(processingStep: clusterJob.processingStep)
        String expected = "Log file: ${ClusterJobLoggingService.logDirectory(clusterJob.realm, clusterJob.processingStep)}/${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}"

        assert AbstractOtpJob.getLogFileNames(clusterJob) == expected
    }
}
