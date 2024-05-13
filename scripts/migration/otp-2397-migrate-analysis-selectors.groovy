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

import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

// input
Pipeline.Name oldPipelineName = Pipeline.Name.RODDY_SNV
String newWorkflowName = SnvWorkflow.WORKFLOW
List<SeqType> seqTypes = [
        SeqTypeService.wholeGenomePairedSeqType,
        SeqTypeService.exomePairedSeqType,
]
/** Run this script w/o modification of database if set to true */
Boolean dryRun = true

// script
MergingCriteriaService mergingCriteriaService = ctx.mergingCriteriaService
WorkflowVersionService workflowVersionService = ctx.workflowVersionService

Pipeline pipeline = exactlyOneElement(Pipeline.findAllByName(oldPipelineName))
Workflow workflow = exactlyOneElement(Workflow.findAllByName(newWorkflowName))
println "Workflow: ${workflow}"
List<WorkflowVersion> workflowVersions = workflowVersionService.findAllByWorkflow(workflow)
println "Workflow versions: ${workflowVersions.join(", ")}"
WorkflowVersionSelector.withTransaction {
    RoddyWorkflowConfig.findAllWhere(
            pipeline: pipeline,
            obsoleteDate: null,
            individual: null,
    ).findAll {
        it.seqType in seqTypes
    }.each { RoddyWorkflowConfig rwc ->

        String version = rwc.programVersion.split(":").last()
        WorkflowVersion workflowVersion = workflowVersions.find { it.workflowVersion == version }
        if (!workflowVersion) {
            println "WARNING: Workflow version '${version}' for '${rwc.project} ${rwc.seqType}' could not be found, skipping."
            return
        }
        println "Workflow version '${version}' for '${rwc.project} ${rwc.seqType}' was found!"
        println "creating WorkflowVersionSelector for '${rwc.project} ${rwc.seqType} ${workflowVersion}'"
        new WorkflowVersionSelector(
                project: rwc.project,
                seqType: rwc.seqType,
                workflowVersion: workflowVersion,
        ).save(flush: true)
    }

    workflow.defaultSeqTypesForWorkflowVersions = seqTypes
    workflow.save(flush: true)

    assert !dryRun: "This is a dry run, w/o modification of database."
}
[]
