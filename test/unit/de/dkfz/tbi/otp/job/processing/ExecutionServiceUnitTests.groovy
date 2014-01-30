package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.job.processing.ExecutionService.ClusterJobStatus

@TestMixin(GrailsUnitTestMixin)
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

    String qstatOutputForJobNotFound() {
        return "qstat: Unknown Job Id 1234567.whateverClusterHead.com"
    }

    void testIsJobPendingWithJobRunning() {
        String output = qstatOutputForJobFound(ClusterJobStatus.RUNNING)
        assertTrue(executionService.isJobPending(output))
    }

    void testIsJobPendingWithJobCompleted() {
        String output = qstatOutputForJobFound(ClusterJobStatus.COMPLETED)
        assertFalse(executionService.isJobPending(output))
    }

    void testIsJobPendingWithJobHeld() {
        String output = qstatOutputForJobFound(ClusterJobStatus.HELD)
        assertTrue(executionService.isJobPending(output))
    }

    void testIsJobPendingWithJobQueued() {
        String output = qstatOutputForJobFound(ClusterJobStatus.QUEUED)
        assertTrue(executionService.isJobPending(output))
    }

    void testIsJobPendingWithJobNotFound() {
        String output = qstatOutputForJobNotFound()
        assertFalse(executionService.isJobPending(output))
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

    void testIsJobStatusAvailableWithInvalidValue() {
        String output = qstatOutputForJobNotFound()
        assertFalse(executionService.isJobStatusAvailable(output))
    }

    void testIsJobStatusAvailableWithValidValues() {
        String output
        ClusterJobStatus.values().each { ClusterJobStatus clusterJobStatus ->
            output = qstatOutputForJobFound(clusterJobStatus)
            assertTrue(executionService.isJobStatusAvailable(output))
        }
    }
}
