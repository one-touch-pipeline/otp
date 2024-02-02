/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Component
@Scope("prototype")
@Slf4j
class MonitoringTestJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {

    SchedulerService schedulerService

    ClusterJob clusterJob
    boolean fail
    boolean executed = false

    MonitoringTestJob(ProcessingStep processingStep, SchedulerService schedulerService, ClusterJob clusterJob, boolean fail) {
        this.processingStep = processingStep
        this.schedulerService = schedulerService
        this.clusterJob = clusterJob
        this.fail = fail
    }

    @Override
    void finished(ClusterJob finishedClusterJob) {
        assert schedulerService.jobExecutedByCurrentThread == this
        assert LogThreadLocal.threadLog == this.log
        assert finishedClusterJob == clusterJob
        executed = true
        if (fail) {
            throw new NumberFormatException(TestConstants.ARBITRARY_MESSAGE)
        }
        succeed()
        schedulerService.doEndCheck(this)
    }

    @Override
    void execute() throws Exception {
        assert schedulerService.jobExecutedByCurrentThread == this
        assert LogThreadLocal.threadLog == this.log
    }
}
