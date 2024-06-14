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
package de.dkfz.tbi.otp.workflowExecution

import grails.converters.JSON
import grails.validation.Validateable
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.TimeFormats

@PreAuthorize("hasRole('ROLE_ADMIN')")
class WorkflowSystemConfigController implements CheckAndCall {

    static allowedMethods = [
            index                : "GET",
            getWorkflows         : "GET",
            getWorkflowVersions  : "GET",
            updateWorkflow       : "POST",
            updateWorkflowVersion: "PATCH",
    ]

    MergingCriteriaService mergingCriteriaService

    ReferenceGenomeService referenceGenomeService

    SeqTypeService seqTypeService

    WorkflowService workflowService

    WorkflowVersionService workflowVersionService

    def index() {
        return [
                refGenomes: referenceGenomeService.list().sort { a, b ->
                    String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name)
                },
                seqTypes  : seqTypeService.list().sort {
                    it.displayNameWithLibraryLayout
                },
        ]
    }

    def getWorkflows() {
        List<Map> workflows = workflowService.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }.collect { Workflow wf ->
            buildWorkflowOutputObject(wf)
        }
        render(workflows as JSON)
    }

    def getWorkflowVersions(Long workflowId) {
        List<Map> workflowVersions = workflowVersionService.findAllByWorkflowId(workflowId)
                .sort()
                .collect { it -> buildWorkflowVersionOutputObject(it) }
        render(workflowVersions as JSON)
    }

    def updateWorkflowVersion(WorkflowVersionUpdateCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            UpdateWorkflowVersionDto updateWorkflowVersionDto = new UpdateWorkflowVersionDto(
                    cmd.workflowVersionId,
                    cmd.comment,
                    cmd.deprecate,
                    cmd.allowedRefGenomes,
                    cmd.supportedSeqTypes,
            )
            WorkflowVersion workflowVersion = workflowVersionService.updateWorkflowVersion(updateWorkflowVersionDto)
            render(buildWorkflowVersionOutputObject(workflowVersion) as JSON)
        }
    }

    def updateWorkflow(WorkflowUpdateCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            UpdateWorkflowDto updateWorkflowDto = new UpdateWorkflowDto(
                    cmd.id,
                    cmd.priority,
                    cmd.maxParallelWorkflows,
                    cmd.enabled,
                    cmd.defaultVersion,
                    cmd.allowedRefGenomes,
                    cmd.supportedSeqTypes,
            )

            Workflow workflow = workflowService.updateWorkflow(updateWorkflowDto)
            Map output = buildWorkflowOutputObject(workflow)
            render(output as JSON)
        }
    }

    private Map buildWorkflowVersionOutputObject(WorkflowVersion wv) {
        return [
                workflowId       : wv.workflow.id,
                id               : wv.id,
                name             : wv.workflowVersion,
                comment          : wv.comment?.comment ?: '',
                allowedRefGenomes: buildReferenceGenomesOutputObject(wv.allowedReferenceGenomes),
                supportedSeqTypes: buildSeqTypesOutputObject(wv.supportedSeqTypes),
                commentData      : [
                        author: wv.comment?.author ?: '',
                        date  : TimeFormats.DATE.getFormattedDate(wv.comment?.modificationDate),
                ],
                deprecateDate    : TimeFormats.DATE.getFormattedLocalDate(wv.deprecatedDate),
        ]
    }

    private List<Map> buildReferenceGenomesOutputObject(Set<ReferenceGenome> rgList) {
        return rgList.collect { ReferenceGenome rg ->
            [
                    id  : rg.id,
                    name: rg.name,
            ]
        }.sort { it.name } as List<Map>
    }

    private List<Map> buildSeqTypesOutputObject(Set<SeqType> seqTypeList) {
        return seqTypeList.collect { SeqType st ->
            [
                    id           : st.id,
                    displayName  : st.displayNameWithLibraryLayout,
            ]
        }.sort { it.displayName } as List<Map>
    }

    /**
     * Converter especially to get the correct time format in output data sets.
     *
     * @return map of workflow data
     */
    private Map buildWorkflowOutputObject(Workflow wf) {
        List<WorkflowVersion> versions = workflowVersionService.findAllByWorkflow(wf).sort()
        List<Map> supportedSeqTypes = buildSeqTypesOutputObject(wf.defaultSeqTypesForWorkflowVersions)
        List<Map> allowedRefGenomes = buildReferenceGenomesOutputObject(wf.defaultReferenceGenomesForWorkflowVersions)

        return [
                id                  : wf.id,
                name                : wf.name,
                priority            : wf.priority,
                enabled             : wf.enabled,
                maxParallelWorkflows: wf.maxParallelWorkflows,
                defaultVersion      : wf.defaultVersion,
                versions            : versions.collect { buildWorkflowVersionOutputObject(it) },
                supportedSeqTypes   : supportedSeqTypes,
                allowedRefGenomes   : allowedRefGenomes,
                deprecationDate     : TimeFormats.DATE.getFormattedLocalDate(wf.deprecatedDate),
        ]
    }
}

class WorkflowUpdateCommand extends UpdateWorkflowDto implements Validateable {
    static constraints = {
        defaultVersion nullable: true
    }
}

class WorkflowVersionUpdateCommand extends UpdateWorkflowVersionDto implements Validateable {
}
