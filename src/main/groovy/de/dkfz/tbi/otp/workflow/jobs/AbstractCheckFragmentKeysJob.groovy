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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.shared.FailedParsingConfigException
import de.dkfz.tbi.otp.workflow.shared.MissingFragmentKeysException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Job to ensure the required keys to exist in the combined configuration of a workflow run
 * All missing keys should be listed in the log and an exception should be thrown
 */
@Component
@Slf4j
abstract class AbstractCheckFragmentKeysJob extends AbstractJob {

    @Autowired
    MessageSourceService messageSourceService

    /**
     * Execute method parses the config stored in the workflow run and check for missing keys
     * returned by each workflows. All the missing keys are saved in the log
     *
     * If missing keys are found MissingFragmentKeysException will be thrown
     */
    @Override
    void execute(WorkflowStep workflowStep) {
        List<String> errorMessages = []
        try {
            // fetch and parse the combined config into JsonObject
            ObjectMapper mapper = new ObjectMapper()
            JsonNode configRoot = mapper.readTree(workflowStep.workflowRun.combinedConfig)
            // loop thru all the required keys and ensure they do exist
            keyPaths.each { String jsonPath ->
                if (findMissingNode(jsonPath, configRoot)) {
                    errorMessages.add(jsonPath)
                }
            }
        } catch (JsonParseException e) {
            throw new FailedParsingConfigException(messageSourceService.createMessage("workflow.job.failedParsingConfig", [
                    workflowRun: workflowStep.workflowRun
            ]), e)
        }

        if (errorMessages.empty) {
            logService.addSimpleLogEntry(workflowStep, messageSourceService.createMessage("workflow.job.checkFragmentKeys.ok"))
        } else {
            String message = messageSourceService.createMessage("workflow.job.checkFragmentKeys.missing", [
                    keyCount    : errorMessages.size(),
                    workflowRun : workflowStep.workflowRun,
            ]) + "\n${errorMessages.join('\n')}"
            logService.addSimpleLogEntry(workflowStep, message)
            throw new MissingFragmentKeysException(message)
        }
        // persist the workflow state if successful
        // Note: failed states will be persisted by {@link JobScheduler.handleException()}
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    /*
     * Follow the given path and check each node if it exists
     *
     * @param jsonPath
     * @param root of the Json tree
     * @return true if any node in the path is missing, otherwise false
     */
    private boolean findMissingNode(String jsonPath, JsonNode root) {
        JsonNode node = root
        return jsonPath.split('/').any {
            node = node.path(it)
            if (node.isMissingNode()) {
                return true
            }
        }
    }

    @Override
    final JobStage getJobStage() {
        return JobStage.CHECK_FRAGMENT_KEYS
    }

    /**
     * Return the required key paths with simplified format of json path/XPath
     * Each node name is separated by slash '/'
     * E.g. "RODDY/cvalues/outputDirectory"
     *
     * @return the required key paths
     */
    abstract Set<String> getKeyPaths()
}
