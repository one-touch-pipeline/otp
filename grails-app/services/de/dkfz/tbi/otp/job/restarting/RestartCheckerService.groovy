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
package de.dkfz.tbi.otp.job.restarting

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*

@CompileDynamic
@Transactional
class RestartCheckerService {

    @Autowired
    ApplicationContext context

    boolean canJobBeRestarted(Job job) {
        return job instanceof AutoRestartableJob
    }

    boolean canWorkflowBeRestarted(ProcessingStep step) {
        JobExecutionPlan plan = step.process.jobExecutionPlan
        StartJob startJob = context.getBean(plan.startJob.bean)
        return startJob instanceof RestartableStartJob
    }

    boolean isJobAlreadyRestarted(ProcessingStep step) {
        return step instanceof RestartedProcessingStep
    }

    boolean isWorkflowAlreadyRestarted(ProcessingStep step) {
        return step.process.restarted
    }
}
