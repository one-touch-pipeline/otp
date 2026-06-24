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
package de.dkfz.tbi.otp.workflow.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.swagger.client.wes.model.RunId
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.MapUtilService
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.nio.file.Path

@Component
@Slf4j
abstract class AbstractExecuteWesPipelineJob extends AbstractExecutePipelineJob {

    final static String DISALLOWED_CHARS = '[^a-zA-Z0-9\\-]'

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WeskitAccessService weskitAccessService

    @Autowired
    ConfigFragmentService configFragmentService

    @Autowired
    WesRunService wesRunService

    @Autowired
    MapUtilService mapUtilService

    @Autowired
    ProcessingOptionService processingOptionService

    /**
     * Return the used execution system, values needs to be known by Weskit, currently possible: NFL for Nextflow and SMK for Snakemake
     *
     * @return workflow type (SnakeMake, Nextflow, ...)
     */
    abstract WesWorkflowType getWorkflowType()

    /**
     * Return the parameters for workflow runs. Each element in the list will be used to trigger an own Weskit call
     *
     * @param WorkflowStep current workflow step
     * @param Path path of the base directory
     * @return a map of map entries, each map entry has a key for the base path ana a value for the parameters
     */
    abstract Map<Path, Map<String, String>> getRunSpecificParameters(WorkflowStep workflowStep, Path basePath)

    /**
     * Check if an Weskit run should be triggered, in most case it should return true
     *
     * @param WorkflowStep current workflow step
     * @return true if job should be triggered at Weskit, otherwise false
     */
    abstract boolean shouldWeskitJobSend(WorkflowStep workflowStep)

    /**
     * Execute the workflow
     *
     * @param workflowStep current workflow step
     */
    @Override
    void execute(WorkflowStep workflowStep) {
        Path basePath = getWorkDirectory(workflowStep)
        fileService.deleteDirectoryContent(basePath)
        logService.addSimpleLogEntry(workflowStep, "Clean up the output directory ${basePath}")

        if (shouldWeskitJobSend(workflowStep)) {
            workflowRunService.markJobAsNotRestartableInSeparateTransaction(workflowStep.workflowRun)

            WesWorkflowType workflowType = this.workflowType

            ObjectMapper mapper = new ObjectMapper()

            Map<Path, Map<String, String>> parameters = getRunSpecificParameters(workflowStep, basePath)
            logService.addSimpleLogEntry(workflowStep, "Create ${parameters.size()} weskit calls")
            String unixGroup = workflowStep.workflowRun.project.unixGroup
            parameters.each { Path path, Map<String, String> parameter ->
                // define directory to store its output
                fileService.createDirectoryRecursivelyAndSetPermissions(path, unixGroup)

                // config should be created each time since it is modified with mergeSortedMaps method
                Map<String, Map<String, String>> config = mapper.readValue(workflowStep.workflowRun.combinedConfig, HashMap)
                String workflowUrl = config[WeskitCheckFragmentKeysJob.WESKIT][WeskitCheckFragmentKeysJob.WORKFLOW_CONFIG_URL]
                String workflowTypeVersion = config[WeskitCheckFragmentKeysJob.WESKIT][WeskitCheckFragmentKeysJob.WORKFLOW_TYPE_VERSION]

                JSONObject mergedParameter = mapUtilService.mergeSortedMaps([config, parameter]) as JSONObject

                WesWorkflowEngineParameter engineParameter = createWesWorkflowEngineParameter(
                        workflowStep,
                        mergedParameter.get(WeskitCheckFragmentKeysJob.WESKIT) as Map<String, String>)
                mergedParameter.remove(WeskitCheckFragmentKeysJob.WESKIT)
                WesWorkflowParameter wesParameter = new WesWorkflowParameter(mergedParameter, engineParameter, workflowType, workflowTypeVersion, path,
                        workflowUrl)
                logService.addSimpleLogEntry(workflowStep, "Call Weskit with ${wesParameter}")
                logService.addSimpleLogEntry(workflowStep, "Work directory ${path}")

                RunId runId = weskitAccessService.runWorkflow(wesParameter)
                String runIdValue = runId.runId
                logService.addSimpleLogEntry(workflowStep, "Workflow job with run id ${runIdValue} has been sent to Weskit")

                Path subPath = basePath.relativize(path)
                wesRunService.saveWorkflowRun(workflowStep, runIdValue, subPath.toString())
            }
            workflowRunService.markJobAsRestartable(workflowStep.workflowRun)
            workflowStateChangeService.changeStateToWaitingOnSystem(workflowStep)
        } else {
            logService.addSimpleLogEntry(workflowStep, "No workflow job sent to Weskit")
            workflowStateChangeService.changeStateToSuccess(workflowStep)
        }
    }

    private WesWorkflowEngineParameter createWesWorkflowEngineParameter(WorkflowStep workflowStep, Map<String, String> weskit) {
        Project project = workflowStep.workflowRun.project
        String accountName = processingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.ENABLE_ACCOUNTING_FOR_WESKIT) ?
                replaceDisallowedChars(project.name) : null
        ProcessingPriority processingPriority = project.processingPriority
        String jobName = createJobName(workflowStep)

        return new WesWorkflowEngineParameter(
                accountName,
                jobName,
                processingPriority.queue,
                weskit.get(WeskitCheckFragmentKeysJob.MAX_MEMORY) as String,
                weskit.get(WeskitCheckFragmentKeysJob.MAX_RUNTIME) as String,
                weskit.get(WeskitCheckFragmentKeysJob.PROFILE) as String,
        )
    }

    String createJobName(WorkflowStep step) {
        String beanName = step.beanName
        String workflowName = replaceDisallowedChars(step.workflowRun.workflow.name)
        String workflowRunName = replaceDisallowedChars(step.workflowRun.shortDisplayName)
        String stepId = step.id
        return [
                'otp',
                workflowName,
                beanName,
                workflowRunName,
                stepId,
        ].findAll().join('_')
    }

    String replaceDisallowedChars(String input) {
        return input.replaceAll(DISALLOWED_CHARS, '-')
    }
}
