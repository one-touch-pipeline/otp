/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import grails.converters.JSON
import org.grails.web.json.JSONElement
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.ClusterJobSubmissionOptionsService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterAccessService

abstract class AbstractExecuteClusterPipelineJob extends AbstractExecutePipelineJob {

    @Autowired
    ClusterAccessService clusterAccessService

    @Override
    void execute(WorkflowStep workflowStep) {
        assert workflowStep.workflowRun.combinedConfig
        List<String> scripts = createScripts(workflowStep)
        if (scripts) {
            logService.addSimpleLogEntry(workflowStep, "${scripts.size()} scripts will be send")
            submitScripts(workflowStep, scripts)
            workflowStateChangeService.changeStateToWaitingOnSystem(workflowStep)
            return
        }
        logService.addSimpleLogEntry(workflowStep, "No scripts to send, job finish successfully")
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    protected abstract List<String> createScripts(WorkflowStep workflowStep)

    private void submitScripts(WorkflowStep workflowStep, List<String> scripts) {
        Map<JobSubmissionOption, String> jobSubmissionOptions = getJobSubmissionOptions(workflowStep.workflowRun)
        clusterAccessService.executeJobs(workflowStep, scripts, jobSubmissionOptions)
    }

    private Map<JobSubmissionOption, String> getJobSubmissionOptions(WorkflowRun run) {
        Map<JobSubmissionOption, String> options = [:]
        JSONElement config = JSON.parse(run.combinedConfig)[ExternalWorkflowConfigFragment.Type.OTP_CLUSTER.name()] as JSONElement
        if (config) {
            options.putAll(ClusterJobSubmissionOptionsService.convertJsonObjectStringToMap(config))
        }
        return options
    }
}
