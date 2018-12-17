package de.dkfz.tbi.otp.job.scheduler

import org.apache.commons.logging.Log
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.jobs.TestJob
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.utils.logging.JobLog
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class SchedulerServiceUnitTests extends TestCase {

    final SchedulerService service = new SchedulerService()

    final Job testJob1 = new TestJob()
    final Job testJob2 = new TestJob()
    final Log testLog1 = new JobLog(null, null)
    final Log testLog2 = new JobLog(null, null)

    /**
     * Initialize the test (setup). This is needed because the tests need to execute everything in one thread but JUnit
     * does not guarantee that setUp() and the test method run in the same thread.
     */
    void initialize() {
        LogThreadLocal.removeThreadLog()
        testJob1.log = testLog1
        testJob2.log = testLog2
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_null() {
        initialize()
        try {
            shouldFail IllegalArgumentException, {
                service.startingJobExecutionOnCurrentThread(null)
            }
            assert service.jobExecutedByCurrentThread == null
            assert LogThreadLocal.threadLog == null
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_logNull() {
        initialize()
        try {
            shouldFail IllegalArgumentException, {
                service.startingJobExecutionOnCurrentThread(new TestJob())
            }
            assert service.jobExecutedByCurrentThread == null
            assert LogThreadLocal.threadLog == null
        } finally {
             LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_twice() {
        initialize()
        try {
            service.startingJobExecutionOnCurrentThread(testJob1)
            assert service.jobExecutedByCurrentThread == testJob1
            assert LogThreadLocal.threadLog == testLog1

            shouldFail IllegalStateException, {
                service.startingJobExecutionOnCurrentThread(testJob1)
            }
            assert service.jobExecutedByCurrentThread == testJob1
            assert LogThreadLocal.threadLog == testLog1
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_twiceOther() {
        initialize()
        try {
            service.startingJobExecutionOnCurrentThread(testJob1)
            assert service.jobExecutedByCurrentThread == testJob1
            assert LogThreadLocal.threadLog == testLog1

            shouldFail IllegalStateException, {
                service.startingJobExecutionOnCurrentThread(testJob2)
            }
            assert service.jobExecutedByCurrentThread == testJob1
            assert LogThreadLocal.threadLog == testLog1
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testStartingJobExecutionOnCurrentThread_threadLogSet() {
        initialize()
        try {
            LogThreadLocal.setThreadLog(testLog1)
            assert service.jobExecutedByCurrentThread == null
            assert LogThreadLocal.threadLog == testLog1

            shouldFail IllegalStateException, {
                service.startingJobExecutionOnCurrentThread(testJob1)
            }
            assert service.jobExecutedByCurrentThread == null
            assert LogThreadLocal.threadLog == testLog1
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_null() {
        initialize()
        try {
            shouldFail IllegalArgumentException, {
                service.finishedJobExecutionOnCurrentThread(null)
            }
            assert service.jobExecutedByCurrentThread == null
            assert LogThreadLocal.threadLog == null
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_notStarted() {
        initialize()
        try {
            shouldFail IllegalStateException, {
                service.finishedJobExecutionOnCurrentThread(testJob2)
            }
            assert service.jobExecutedByCurrentThread == null
            assert LogThreadLocal.threadLog == null
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_differentJob() {
        initialize()
        try {
            service.startingJobExecutionOnCurrentThread(testJob1)
            assert service.jobExecutedByCurrentThread == testJob1
            assert LogThreadLocal.threadLog == testLog1

            shouldFail IllegalStateException, {
                service.finishedJobExecutionOnCurrentThread(testJob2)
            }
            assert service.jobExecutedByCurrentThread == testJob1
            assert LogThreadLocal.threadLog == testLog1
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_differentLog() {
        initialize()
        try {
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
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testFinishedJobExecutionOnCurrentThread_twice() {
        initialize()
        try {
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
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testStartingAndFinishedJobExecutionOnCurrentThread_success() {
        initialize()
        try {
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
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }
}
