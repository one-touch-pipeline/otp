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
package migration

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.*

List<Project> projectList = Project.findAllByProjectType(Project.ProjectType.SEQUENCING)

String defaultFastQcType = ctx.processingOptionService.findOptionAsString(ProcessingOption.OptionName.DEFAULT_FASTQC_TYPE)
println "The defaultFastQcType is: ${defaultFastQcType}"

Workflow workflow
WorkflowVersion workflowVersion

if (defaultFastQcType) {
    workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByNameIlike(defaultFastQcType + " fastqc"))
    workflowVersion = CollectionUtils.exactlyOneElement(WorkflowVersion.createCriteria().list(max: 1) {
        eq("workflow", workflow)
        order("lastUpdated", "desc")
    } as Collection<Object>) as WorkflowVersion
}

Project.withTransaction {
    if (workflowVersion) {
        println "The following projects would be updated with workflowVersion: ${workflowVersion}"
        projectList.each { Project project ->
            if (WorkflowVersionSelector.findByProjectAndWorkflowVersion(project, workflowVersion)) {
                println "skip: ${project}"
            } else {
                println "configure: ${project}"
                new WorkflowVersionSelector(project: project, workflowVersion: workflowVersion).save(flush: true)
            }
        }
    }
    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}
