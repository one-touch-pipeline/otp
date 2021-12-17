/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.scheduler

import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import spock.lang.Specification

import de.dkfz.tbi.otp.job.jobs.TestJob
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class SchedulerServiceSpec extends Specification {

    final SchedulerService service = new SchedulerService()

    final Job testJob1 = new TestJob()
    final Job testJob2 = new TestJob()

    /**
     * Initialize the test (setup). This is needed because the tests need to execute everything in one thread but JUnit
     * does not guarantee that setUp() and the test method run in the same thread.
     */
    void initialize() {
        LogThreadLocal.removeThreadLog()
        assert service.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
    }

    void cleanup() {
        LogThreadLocal.removeThreadLog()
    }

    void testStartingJobExecutionOnCurrentThread_null() {
        given:
        initialize()

        when:
        service.startingJobExecutionOnCurrentThread(null)

        then:
        thrown(AssertionError)
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == null
    }

    void testStartingJobExecutionOnCurrentThread_twice() {
        given:
        initialize()

        when:
        service.startingJobExecutionOnCurrentThread(testJob1)

        then:
        service.jobExecutedByCurrentThread == testJob1
        LogThreadLocal.threadLog == testJob1.log

        when:
        service.startingJobExecutionOnCurrentThread(testJob1)

        then:
        thrown(IllegalStateException)
        service.jobExecutedByCurrentThread == testJob1
        LogThreadLocal.threadLog == testJob1.log
    }

    void testStartingJobExecutionOnCurrentThread_twiceOther() {
        given:
        initialize()

        when:
        service.startingJobExecutionOnCurrentThread(testJob1)

        then:
        service.jobExecutedByCurrentThread == testJob1
        LogThreadLocal.threadLog == testJob1.log

        when:
        service.startingJobExecutionOnCurrentThread(testJob2)

        then:
        thrown(IllegalStateException)
        service.jobExecutedByCurrentThread == testJob1
        LogThreadLocal.threadLog == testJob1.log
    }

    void testStartingJobExecutionOnCurrentThread_threadLogSet() {
        given:
        initialize()

        when:
        LogThreadLocal.threadLog = testJob1.log

        then:
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == testJob1.log

        when:
        service.startingJobExecutionOnCurrentThread(testJob1)

        then:
        thrown(IllegalStateException)
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == testJob1.log
    }

    void testFinishedJobExecutionOnCurrentThread_null() {
        given:
        initialize()

        when:
        service.finishedJobExecutionOnCurrentThread(null)

        then:
        thrown(IllegalArgumentException)
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == null
    }

    void testFinishedJobExecutionOnCurrentThread_notStarted() {
        given:
        initialize()

        when:
        service.finishedJobExecutionOnCurrentThread(testJob2)

        then:
        thrown(IllegalStateException)
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == null
    }

    void testFinishedJobExecutionOnCurrentThread_differentJob() {
        given:
        initialize()

        when:
        service.startingJobExecutionOnCurrentThread(testJob1)

        then:
        service.jobExecutedByCurrentThread == testJob1
        LogThreadLocal.threadLog == testJob1.log

        when:
        service.finishedJobExecutionOnCurrentThread(testJob2)

        then:
        thrown(IllegalStateException)
        service.jobExecutedByCurrentThread == testJob1
        LogThreadLocal.threadLog == testJob1.log
    }

    void testFinishedJobExecutionOnCurrentThread_differentLog() {
        given:
        initialize()
        Logger log = new NOPLogger()

        when:
        service.startingJobExecutionOnCurrentThread(testJob2)

        then:
        service.jobExecutedByCurrentThread == testJob2
        LogThreadLocal.threadLog == testJob2.log

        when:
        LogThreadLocal.threadLog = log

        then:
        service.jobExecutedByCurrentThread == testJob2
        LogThreadLocal.threadLog == log

        when:
        service.finishedJobExecutionOnCurrentThread(testJob2)

        then:
        thrown(IllegalStateException)
        service.jobExecutedByCurrentThread == testJob2
        LogThreadLocal.threadLog == log
    }

    void testFinishedJobExecutionOnCurrentThread_twice() {
        given:
        initialize()

        when:
        service.startingJobExecutionOnCurrentThread(testJob2)

        then:
        service.jobExecutedByCurrentThread == testJob2
        LogThreadLocal.threadLog == testJob2.log

        when:
        service.finishedJobExecutionOnCurrentThread(testJob2)

        then:
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == null

        when:
        service.finishedJobExecutionOnCurrentThread(testJob2)

        then:
        thrown(IllegalStateException)
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == null
    }

    void testStartingAndFinishedJobExecutionOnCurrentThread_success() {
        given:
        initialize()

        when:
        service.startingJobExecutionOnCurrentThread(testJob1)

        then:
        service.jobExecutedByCurrentThread == testJob1
        LogThreadLocal.threadLog == testJob1.log

        when:
        service.finishedJobExecutionOnCurrentThread(testJob1)

        then:
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == null

        when:
        service.startingJobExecutionOnCurrentThread(testJob2)

        then:
        service.jobExecutedByCurrentThread == testJob2
        LogThreadLocal.threadLog == testJob2.log

        when:
        service.finishedJobExecutionOnCurrentThread(testJob2)

        then:
        service.jobExecutedByCurrentThread == null
        LogThreadLocal.threadLog == null
    }
}
