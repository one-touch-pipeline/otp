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
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.JobExecutionPlanChangedEvent
import de.dkfz.tbi.otp.job.processing.StartJob

/**
 * Very simple Test implementation of the StartJob interface.
 * Does nothing useful.
 * This is similar to TestStartJob with the difference that it is a singleton
 * instance and allows to change the JobExecutionPlan during testing. For that
 * it does not inherit from AbstractStartJob, but just implements the StartJob
 * interface.
 */
@Component("testSingletonStartJob")
@Scope("singleton")
@Slf4j
class TestSingletonStartJob implements StartJob, ApplicationListener<JobExecutionPlanChangedEvent> {
    private JobExecutionPlan plan
    private boolean planNeedsRefresh = false

    TestSingletonStartJob() { }

    TestSingletonStartJob(JobExecutionPlan plan) {
        this.plan = plan
    }

    @Override
    JobExecutionPlan getJobExecutionPlan() {
        if (planNeedsRefresh) {
            plan = JobExecutionPlan.get(plan.id)
            planNeedsRefresh = false
        }
        return plan
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    void execute() { }

    void setExecutionPlan(JobExecutionPlan plan) {
        this.plan = JobExecutionPlan.get(plan?.id)
    }

    void onApplicationEvent(JobExecutionPlanChangedEvent event) {
        if (this.plan && event.planId == this.plan.id) {
            planNeedsRefresh = true
        }
    }
}
