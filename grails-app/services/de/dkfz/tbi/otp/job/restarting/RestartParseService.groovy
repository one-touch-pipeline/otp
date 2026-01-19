/*
 * Copyright 2011-2026 The OTP authors
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

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.plan.JobErrorDefinition
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * @deprecated class is part of the old workflow system
 */
@Transactional
class RestartParseService {

    static final int MEGABYTE = 1000 * 1000

    static final int THRESHOLD_IN_MB = 10

    static final int THRESHOLD_IN_BYTE = THRESHOLD_IN_MB * MEGABYTE

    ErrorLogService errorLogService

    SchedulerService schedulerService

    @CompileDynamic
    JobErrorDefinition.Action handleTypeMessage(Job job, Collection<JobErrorDefinition> jobErrorDefinitions) {
        job.log.debug("Checking error message.")
        ProcessingError error = job.processingStep.latestProcessingStepUpdate.error
        return extractMatchingAction(job, 'error message', error.errorMessage, jobErrorDefinitions)
    }

    @CompileDynamic
    JobErrorDefinition.Action handleTypeStackTrace(Job job, Collection<JobErrorDefinition> jobErrorDefinitions) {
        job.log.debug("Checking stacktrace.")
        ProcessingError error = job.processingStep.latestProcessingStepUpdate.error
        File file = errorLogService.getStackTracesFile(error.stackTraceIdentifier)
        if (!file.exists()) {
            job.log.debug("The stack trace file '${file}' does not exist.")
            return null
        }
        return extractMatchingAction(job, 'stack trace', file.text, jobErrorDefinitions)
    }

    @CompileDynamic
    JobErrorDefinition.Action handleTypeClusterLogs(Job job, Collection<JobErrorDefinition> jobErrorDefinitions) {
        job.log.debug("Checking cluster job log(s).")
        Collection<ClusterJob> clusterJobs = job.failedOrNotFinishedClusterJobs()

        if (clusterJobs.empty) {
            job.log.debug("Could not find any cluster job log.")
            return JobErrorDefinition.Action.STOP
        }

        List<JobErrorDefinition.Action> actions = clusterJobs.collect { ClusterJob clusterJob ->
            File file = clusterJob.jobLog as File
            if (!file) {
                job.log.debug("Log file for '${clusterJob.clusterJobId}' is null, will be skipped.")
                return null
            }
            if (!file.exists()) {
                job.log.debug("Log file '${file}' for '${clusterJob.clusterJobId}' does not exist in file system, will be skipped.")
                return null
            } else if (file.size() > THRESHOLD_IN_BYTE) {
                job.log.debug("Stopping because log file '${file}' for '${clusterJob.clusterJobId}' is too big for processing " +
                        "(file size: ${(int) (file.size() / MEGABYTE)} MB, threshold: ${THRESHOLD_IN_MB} MB).")
                return JobErrorDefinition.Action.STOP
            }
            return extractMatchingAction(job, file.path, file.text, jobErrorDefinitions)
        }.findAll().unique()

        switch (actions.size()) {
            case 0:
                job.log.debug("For none of the cluster job logs a matching rule could be found.")
                return null
            case 1:
                job.log.debug("Found action ${actions.first()} for cluster job logs.")
                return actions.first()
            default:
                job.log.debug("Stopping, because multiple rules found for cluster job logs: \n- ${actions.join('\n- ')}")
                return JobErrorDefinition.Action.STOP
        }
    }

    @SuppressWarnings("ThrowRuntimeException")
    // ignored: will be removed with the old workflow system
    JobErrorDefinition.Action detectAndHandleType(Job job, Collection<JobErrorDefinition> jobErrorDefinitions) {
        JobErrorDefinition.Type type = CollectionUtils.exactlyOneElement(jobErrorDefinitions*.type.unique())

        switch (type) {
            case JobErrorDefinition.Type.MESSAGE:
                return handleTypeMessage(job, jobErrorDefinitions)

            case JobErrorDefinition.Type.STACKTRACE:
                return handleTypeStackTrace(job, jobErrorDefinitions)

            case JobErrorDefinition.Type.CLUSTER_LOG:
                return handleTypeClusterLogs(job, jobErrorDefinitions)

            default:
                throw new RuntimeException("Unknown message type: ${type}")
        }
    }

    @CompileDynamic
    JobErrorDefinition.Action extractMatchingAction(Job job, String it, String text, Collection<JobErrorDefinition> jobErrorDefinitions) {
        Collection<JobErrorDefinition> matching = jobErrorDefinitions.findAll {
            text =~ it.errorExpression
        }

        switch (matching.size()) {
            case 0:
                job.log.debug("No matching rule found for ${it}.")
                return null
            case 1:
                JobErrorDefinition definition = CollectionUtils.exactlyOneElement(matching)
                job.log.debug("Rule ${definition} matches ${it}.")
                if (definition.action == JobErrorDefinition.Action.CHECK_FURTHER) {
                    if (definition.checkFurtherJobErrors.isEmpty()) {
                        job.log.debug("Action of ${definition} is \"CHECK_FURTHER\" but \"checkFurtherJobErrors\" is empty")
                        return JobErrorDefinition.Action.STOP
                    }
                    return detectAndHandleType(job, definition.checkFurtherJobErrors)
                }
                return definition.action
            default:
                job.log.debug("Stopping, because multiple rules match ${it}: \n- ${jobErrorDefinitions.join('\n- ')}")
                return JobErrorDefinition.Action.STOP
        }
    }
}
