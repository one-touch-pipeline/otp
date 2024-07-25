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

import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.*

/**
 * Script to check the cvalues of alignment workflows for a given list of projects.
 */

//-------------
// input area

/**
 * Provide projects, seperated by newline.
 * empty lines and lines starting with '#' are ignored.
 */
String projectString = """
#project 1
#project 2

"""

//-------------
// work area

List<Project> projects = projectString.split("\n")*.trim().findAll {
    it && !it.startsWith("#")
}.collect {
    CollectionUtils.exactlyOneElement(Project.findAllByName(it), "No Project with name '${it}' exist")
}

Map<String, Map<String, Map<String, List<String>>>> checkedOutput = [:].withDefault { [:].withDefault { [:].withDefault { [] } } }

assert projects: "No projects given"

WorkflowService workflowService = ctx.workflowService
WorkflowVersionSelectorService workflowVersionSelectorService = ctx.workflowVersionSelectorService
ConfigSelectorService configSelectorService = ctx.configSelectorService
ConfigFragmentService configFragmentService = ctx.configFragmentService

Map<String, List<String>> configsPerWorkflow = [:].withDefault { [] }

List<Workflow> alignmentWorkflows = workflowService.findAllAlignmentWorkflows().sort {
    it.id
}

projects.each { Project project ->
    println "Project: ${project.name}"

    alignmentWorkflows.each { Workflow workflow ->
        println "  Workflow: ${workflow.name}"

        List<WorkflowVersionSelector> workflowVersionSelectors = workflowVersionSelectorService.findAllByProjectAndWorkflow(project, workflow)
        workflowVersionSelectors.each { WorkflowVersionSelector wvSelector ->
            println "    wvSelector: ${wvSelector}"

            SingleSelectSelectorExtendedCriteria extendedCriteria = new SingleSelectSelectorExtendedCriteria(
                    workflow,
                    wvSelector.workflowVersion,
                    project,
                    wvSelector.seqType,
                    null,
                    null
            )

            List<ExternalWorkflowConfigSelector> selectors = configSelectorService.findAllSelectorsSortedByPriority(extendedCriteria)
            List<ExternalWorkflowConfigFragment> fragments = selectors*.externalWorkflowConfigFragment
            JSONObject fragmentJson = configFragmentService.mergeSortedFragmentsAsJson(fragments)

            JSONObject cvalue = fragmentJson.RODDY.cvalues

            checkedOutput[workflow.name]['plugin version'][wvSelector.workflowVersion.workflowVersion] << "${project.name} ${wvSelector.seqType}"

            String projectSeqType = "${project.name} ${wvSelector.seqType}"
            configsPerWorkflow[workflow.name] << projectSeqType

            cvalue.each { String key, Map map ->
                String value = map.value
                checkedOutput[workflow.name][key][value] << projectSeqType
            }
        }
    }
}

println '-' * 40

checkedOutput.each { String workflowName, Map<String, List<String>> valuePerWorkflow ->
    println "Workflow: ${workflowName}"
    List<String> configList = configsPerWorkflow[workflowName]
    int workflowCount = configList.size()

    valuePerWorkflow.each { String key, Map<String, List<String>> values ->
        println "  Key: ${key}"
        values.each { String value, List<String> whereList ->
            println "    Value: ${value}: ${whereList.sort().join(", ")}"
        }
        List<String> projectEntries = values.values().flatten()
        if (projectEntries.size() != workflowCount) {
            List copy = configList.clone()
            copy.removeAll(projectEntries)
            println "    No value provided by OTP: ${copy}"
        }
    }
}
''
