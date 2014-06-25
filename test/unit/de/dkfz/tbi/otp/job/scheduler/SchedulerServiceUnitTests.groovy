package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.jobs.TestJob
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.utils.logging.JobLog
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.apache.commons.logging.Log
import org.junit.*

class SchedulerServiceUnitTests extends TestCase {

    final SchedulerService service = new SchedulerService()

    final Job testJob1 = new TestJob()
    final Job testJob2 = new TestJob()
    final Log testLog1 = new JobLog(null, null)
    final Log testLog2 = new JobLog(null, null)

    @Before
    void setUp() {
        LogThreadLocal.removeThreadLog()
        testJob1.log = testLog1
        testJob2.log = testLog2
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_null() {
        shouldFail IllegalArgumentException, {
            service.startingJobExecutionOnCurrentThread(null)
        }
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_logNull() {
        shouldFail IllegalArgumentException, {
            service.startingJobExecutionOnCurrentThread(new TestJob())
        }
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_twice() {

        service.startingJobExecutionOnCurrentThread(testJob1)
        assert service.jobExecutedByCurrentThread == testJob1
        assert LogThreadLocal.threadLog == testLog1

        shouldFail IllegalStateException, {
            service.startingJobExecutionOnCurrentThread(testJob1)
        }
        assert service.jobExecutedByCurrentThread == testJob1
        assert LogThreadLocal.threadLog == testLog1
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_twiceOther() {

        service.startingJobExecutionOnCurrentThread(testJob1)
        assert service.jobExecutedByCurrentThread == testJob1
        assert LogThreadLocal.threadLog == testLog1

        shouldFail IllegalStateException, {
            service.startingJobExecutionOnCurrentThread(testJob2)
        }
        assert service.jobExecutedByCurrentThread == testJob1
        assert LogThreadLocal.threadLog == testLog1
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_threadLogSet() {

        LogThreadLocal.setThreadLog(testLog1)
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == testLog1

        shouldFail IllegalStateException, {
            service.startingJobExecutionOnCurrentThread(testJob1)
        }
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == testLog1
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_null() {
        shouldFail IllegalArgumentException, {
            service.finishedJobExecutionOnCurrentThread(null)
        }
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_notStarted() {
        shouldFail IllegalStateException, {
            service.finishedJobExecutionOnCurrentThread(testJob2)
        }
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_differentJob() {

        service.startingJobExecutionOnCurrentThread(testJob1)
        assert service.jobExecutedByCurrentThread == testJob1
        assert LogThreadLocal.threadLog == testLog1

        shouldFail IllegalStateException, {
            service.finishedJobExecutionOnCurrentThread(testJob2)
        }
        assert service.jobExecutedByCurrentThread == testJob1
        assert LogThreadLocal.threadLog == testLog1
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_differentLog() {

        service.startingJobExecutionOnCurrentThread(testJob2)
        assert service.jobExecutedByCurrentThread == testJob2
        assert LogThreadLocal.threadLog == testLog2

        LogThreadLocal.threadLog = testLog1
        assert service.jobExecutedByCurrentThread == testJob2
        assert LogThreadLocal.threadLog == testLog1

        shouldFail IllegalStateException, {
            service.finishedJobExecutionOnCurrentThread(testJob2)
        }
        assert service.jobExecutedByCurrentThread == testJob2
        assert LogThreadLocal.threadLog == testLog1
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_twice() {

        service.startingJobExecutionOnCurrentThread(testJob2)
        assert service.jobExecutedByCurrentThread == testJob2
        assert LogThreadLocal.threadLog == testLog2

        service.finishedJobExecutionOnCurrentThread(testJob2)
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null

        shouldFail IllegalStateException, {
            service.finishedJobExecutionOnCurrentThread(testJob2)
        }
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    @Test
    void testStartingAndFinishedJobExecutionOnCurrentThread_success() {

        service.startingJobExecutionOnCurrentThread(testJob1)
        assert service.jobExecutedByCurrentThread == testJob1
        assert LogThreadLocal.threadLog == testLog1

        service.finishedJobExecutionOnCurrentThread(testJob1)
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null

        service.startingJobExecutionOnCurrentThread(testJob2)
        assert service.jobExecutedByCurrentThread == testJob2
        assert LogThreadLocal.threadLog == testLog2

        service.finishedJobExecutionOnCurrentThread(testJob2)
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }
}
