package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionService.ClusterJobStatus

@TestMixin(GrailsUnitTestMixin)
@Mock([JobExecutionPlan, Process, ProcessingStep])
class ExecutionServiceUnitTests {

    ExecutionService executionService = new ExecutionService()

    String qstatOutputForJobFound(String jobId, String jobName, String user, String timeUse, String status, String queueName) {
        String outputRunning = """
        Job ID                    Name             User            Time Use S Queue
        ------------------------- ---------------- --------------- -------- - -----
        ${jobId}           ${jobName}            ${user}         ${timeUse} ${status} ${queueName}"""
    }

    String qstatOutputForJobFound(ClusterJobStatus clusterJobStatus) {
        return qstatOutputForJobFound(
        "anyJobId",
        "anyJobName",
        "anyUser",
        "anyTime",
        clusterJobStatus.value,
        "anyQueue")
    }

    void testExistingJobStatusWithStatusHeld() {
        ClusterJobStatus clusterJobStatus = ClusterJobStatus.HELD
        String output = qstatOutputForJobFound(clusterJobStatus)
        assertTrue(executionService.existingJobStatus(output) == clusterJobStatus.value)
    }

    void testExistingJobStatusWithStatusRunning() {
        ClusterJobStatus clusterJobStatus = ClusterJobStatus.RUNNING
        String output = qstatOutputForJobFound(clusterJobStatus)
        assertTrue(executionService.existingJobStatus(output) == clusterJobStatus.value)
    }

    void testExistingJobStatusWithStatusCompleted() {
        ClusterJobStatus clusterJobStatus = ClusterJobStatus.COMPLETED
        String output = qstatOutputForJobFound(clusterJobStatus)
        assertTrue(executionService.existingJobStatus(output) == clusterJobStatus.value)
    }

    void testExistingJobStatusWithStatusQueued() {
        ClusterJobStatus clusterJobStatus = ClusterJobStatus.QUEUED
        String output = qstatOutputForJobFound(clusterJobStatus)
        assertTrue(executionService.existingJobStatus(output) == clusterJobStatus.value)
    }
}
