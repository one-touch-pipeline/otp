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

import de.dkfz.tbi.otp.job.plan.JobErrorDefinition
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.ProcessingStep

@CompileDynamic
@Transactional
class RestartHandlerService {

    RestartCheckerService restartCheckerService

    RestartParseService restartParseService

    RestartActionService restartActionService

    void handleRestart(Job job) {
        ProcessingStep step = job.processingStep
        step.attach()

        job.log.debug("Starting auto-restart handler.")

        if (restartCheckerService.isJobAlreadyRestarted(step)) {
            job.log.debug("Stopping, because job has already been restarted.")
            return
        }

        if (restartCheckerService.isWorkflowAlreadyRestarted(step)) {
            job.log.debug("Stopping, because workflow has already been restarted.")
            return
        }

        if (!(restartCheckerService.canJobBeRestarted(job) || restartCheckerService.canWorkflowBeRestarted(step))) {
            job.log.debug("Stopping, because neither job nor workflow are restartable.")
            return
        }

        List<JobErrorDefinition> jobErrorDefinitions = JobErrorDefinition.createCriteria().list {
            jobDefinitions {
                eq('id', step.jobDefinition.id)
            }
            eq('type', JobErrorDefinition.Type.MESSAGE)
        }

        if (jobErrorDefinitions.empty) {
            job.log.debug("Stopping, because no error patterns are defined for the job.")
            return
        }

        JobErrorDefinition.Action action = restartParseService.handleTypeMessage(job, jobErrorDefinitions)

        restartActionService.handleAction(action, job)
    }
}
