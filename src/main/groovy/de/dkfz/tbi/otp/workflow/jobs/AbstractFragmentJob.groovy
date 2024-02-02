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
package de.dkfz.tbi.otp.workflow.jobs

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.workflow.shared.MultipleCombinedFragmentException
import de.dkfz.tbi.otp.workflowExecution.*

/**
 * Fetch the fragments for a run
 */
@Slf4j
abstract class AbstractFragmentJob extends AbstractJob {

    public static final int JSON_INDENT_FACTOR = 4

    @Autowired
    ConfigFragmentService configFragmentService

    @Autowired
    WorkflowRunService workflowRunService

    @Override
    final void execute(WorkflowStep workflowStep) {
        List<SingleSelectSelectorExtendedCriteria> criteriaList = fetchSelectors(workflowStep)
        logService.addSimpleLogEntry(workflowStep, "Found ${criteriaList.size()} selectors")
        List<JSONObject> jsonObjects = criteriaList.collect { SingleSelectSelectorExtendedCriteria criteria ->
            logService.addSimpleLogEntry(workflowStep, "Handle ${criteria}")
            List<ExternalWorkflowConfigSelector> selectors = configFragmentService.getSortedFragmentSelectors(criteria)
            logService.addSimpleLogEntry(workflowStep, "Found ${selectors.size()} fragments")
            List<ExternalWorkflowConfigFragment> fragments = selectors.collect { ExternalWorkflowConfigSelector selector ->
                ExternalWorkflowConfigFragment fragment = selector.externalWorkflowConfigFragment
                String value = fragment.configValues
                JSONObject jsonObject = JSON.parse(value) as JSONObject
                String fragmentText = [
                        "name: ${selector.name}",
                        "priority: ${selector.priority}",
                        "fragment:\n${jsonObject.toString(JSON_INDENT_FACTOR)}",
                ].join('\n')
                logService.addSimpleLogEntry(workflowStep, fragmentText)
                return fragment
            }
            JSONObject combinedConfig = configFragmentService.mergeSortedFragmentsAsJson(fragments)
            logService.addSimpleLogEntry(workflowStep, "CombinedFragments:\n${combinedConfig.toString(JSON_INDENT_FACTOR)}")
            return combinedConfig
        }
        Set<JSONObject> uniqueJsonObjects = jsonObjects as Set
        if (uniqueJsonObjects.size() > 1) {
            String message = "Found ${uniqueJsonObjects.size()} different combined fragments"
            logService.addSimpleLogEntry(workflowStep, message)
            throw new MultipleCombinedFragmentException(message)
        }
        workflowRunService.saveCombinedConfig(workflowStep.workflowRun.id, jsonObjects ? jsonObjects.first().toString() : '{}')

        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    final JobStage getJobStage() {
        return JobStage.FETCH_FRAGMENTS
    }

    abstract protected List<SingleSelectSelectorExtendedCriteria> fetchSelectors(WorkflowStep workflowStep)
}
