/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.util.Environment

import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.tbi.otp.job.processing.ClusterJobSubmissionOptionsService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.time.Duration

/**
 * A helper service for {@link ClusterJobHandlingService}. Its only separate to simplify testing.
 */
@GrailsCompileStatic
@Transactional
class ClusterJobHelperService {

    ClusterJobSubmissionOptionsService clusterJobSubmissionOptionsService

    Map<JobSubmissionOption, String> mergeResources(ProcessingPriority processingPriority, Realm realm, Map<JobSubmissionOption, String> jobSubmissionOptions) {
        Map<JobSubmissionOption, String> options = clusterJobSubmissionOptionsService.readDefaultOptions(realm)

        options.putAll(jobSubmissionOptions)

        options.put(
                JobSubmissionOption.QUEUE,
                processingPriority.queue
        )

        return options
    }

    ResourceSet createResourceSet(Map<JobSubmissionOption, String> options) {
        return new ResourceSet(
                options.get(JobSubmissionOption.MEMORY) ? new BufferValue(options.get(JobSubmissionOption.MEMORY)) : null,
                options.get(JobSubmissionOption.CORES) as Integer,
                options.get(JobSubmissionOption.NODES) as Integer,
                (Duration) (options.get(JobSubmissionOption.WALLTIME) ? Duration.parse(options.get(JobSubmissionOption.WALLTIME)) : null),
                options.get(JobSubmissionOption.STORAGE) ? new BufferValue(options.get(JobSubmissionOption.STORAGE)) : null,
                options.get(JobSubmissionOption.QUEUE),
                options.get(JobSubmissionOption.NODE_FEATURE),
        )
    }

    String wrapScript(String script, String logFile, String logMessage) {
        return """\
            |# OTP: Fail on first non-zero exit code
            |set -e

            |umask 0027
            |date +%Y-%m-%d-%H-%M
            |echo \$HOST

            |# BEGIN ORIGINAL SCRIPT
            |${script}
            |# END ORIGINAL SCRIPT

            |touch "${logFile}"
            |chmod 0640 "${logFile}"
            |echo "${logMessage}" >> "${logFile}"
            |""".stripMargin()
    }

    String constructJobName(WorkflowStep step) {
        String env
        switch (Environment.current) {
            case Environment.PRODUCTION:
                env = 'prod'
                break
            case Environment.DEVELOPMENT:
                env = 'devel'
                break
            default:
                env = Environment.current.name.toLowerCase()
        }
        String stepId = step.id
        String beanName = step.beanName
        String workflowName = step.workflowRun.workflow.name
        return [
                'otp',
                env,
                workflowName,
                beanName,
                stepId,
        ].findAll().join('_')
    }
}
