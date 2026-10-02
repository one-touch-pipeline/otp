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
package de.dkfz.tbi.otp.workflowExecution

import grails.converters.JSON
import grails.validation.Validateable
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.TimeFormats
import de.dkfz.tbi.otp.workflow.Version

@PreAuthorize("hasRole('ROLE_ADMIN')")
class WorkflowSystemConfigController implements CheckAndCall {

    static allowedMethods = [
            index                     : "GET",
            getWorkflows              : "GET",
            getWorkflowVersions       : "GET",
            updateWorkflow            : "POST",
            updateWorkflowVersion     : "PATCH",
            getWorkflowDefaultGroups  : "GET",
            saveWorkflowDefaultGroup  : "POST",
            deleteWorkflowDefaultGroup: "POST",
    ]

    MergingCriteriaService mergingCriteriaService

    ReferenceGenomeService referenceGenomeService

    SeqTypeService seqTypeService

    WorkflowService workflowService

    WorkflowVersionService workflowVersionService

    WorkflowDefaultGroupService workflowDefaultGroupService

    def index() {
        return [
                refGenomes       : referenceGenomeService.list().sort { ReferenceGenome a, ReferenceGenome b ->
                    (a.legacy <=> b.legacy) ?: a.name.compareToIgnoreCase(b.name)
                },
                seqTypes         : seqTypeService.list().sort {
                    it.displayNameWithLibraryLayout
                },
                analysisWorkflows: workflowService.findAllAnalysisWorkflows().sort { it.displayName },
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

    def getWorkflowDefaultGroups() {
        List<WorkflowDefaultGroup> groups = workflowDefaultGroupService.list()
        Map<Long, List<WorkflowVersionSelectorDefault>> entriesByGroupId =
                workflowDefaultGroupService.findAllEntries().groupBy { it.group.id }
        render(groups.collectMany { buildWorkflowDefaultGroupRows(it, entriesByGroupId[it.id] ?: []) } as JSON)
    }

    def saveWorkflowDefaultGroup(SaveWorkflowDefaultGroupCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            WorkflowDefaultGroup group = workflowDefaultGroupService.saveGroup(cmd.group, cmd.name, cmd.seqType, cmd.workflowVersions)
            render(buildWorkflowDefaultGroupOutputObject(group) as JSON)
        }
    }

    def deleteWorkflowDefaultGroup(DeleteWorkflowDefaultGroupCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            workflowDefaultGroupService.delete(cmd.group)
            render([] as JSON)
        }
    }

    private Map buildWorkflowDefaultGroupOutputObject(WorkflowDefaultGroup group) {
        return [
                id     : group.id,
                name   : group.name,
                seqType: [id: group.seqType.id, displayName: group.seqType.displayNameWithLibraryLayout],
                entries: workflowDefaultGroupService.findEntries(group).collect { buildWorkflowDefaultEntryOutputObject(it) },
        ]
    }

    /**
     * Flattens a group into one row per entry, ready for the defaults DataTable: each row carries
     * the group's own fields (for the rowspan-merged name/edit columns) alongside that entry's
     * workflow/version. The edit modal rebuilds a group's full entry list client-side by matching
     * `groupId` across rows, rather than this endpoint repeating it in every sibling row.
     */
    private List<Map> buildWorkflowDefaultGroupRows(WorkflowDefaultGroup group, List<WorkflowVersionSelectorDefault> groupEntries) {
        Map seqType = [id: group.seqType.id, displayName: group.seqType.displayNameWithLibraryLayout]

        return groupEntries.collect { buildWorkflowDefaultEntryOutputObject(it) }.collect { entry ->
            [
                    groupId        : group.id,
                    name           : group.name,
                    seqType        : seqType,
                    workflow       : entry.workflow,
                    workflowVersion: entry.workflowVersion,
            ]
        }
    }

    private Map buildWorkflowDefaultEntryOutputObject(WorkflowVersionSelectorDefault entry) {
        return [
                id             : entry.id,
                workflow       : [id: entry.workflowVersion.workflow.id, displayName: entry.workflowVersion.workflow.displayName],
                workflowVersion: [id: entry.workflowVersion.id, displayName: entry.workflowVersion.workflowVersion],
                referenceGenome: entry.referenceGenome ? [id: entry.referenceGenome.id, displayName: entry.referenceGenome.displayName] : null,
                species        : entry.species.collect { [id: it.id, displayName: it.displayName] },
        ]
    }

    private Map buildWorkflowVersionOutputObject(WorkflowVersion wv) {
        Version version = Version.fromWorkflowVersion(wv)

        return [
                workflowId       : wv.workflow.id,
                id               : wv.id,
                name             : wv.workflowVersion,
                displayName      : version.nameWithDefault,
                isDefault        : version.isDefault,
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
        return rgList.sort { ReferenceGenome a, ReferenceGenome b ->
            (a.legacy <=> b.legacy) ?: a.name.compareToIgnoreCase(b.name)
        }.collect { ReferenceGenome rg ->
            [
                    id         : rg.id,
                    displayName: rg.displayName,
            ]
        } as List<Map>
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

class SaveWorkflowDefaultGroupCommand implements Validateable {
    WorkflowDefaultGroup group
    String name
    SeqType seqType
    List<Long> workflowVersion = []

    Set<WorkflowVersion> getWorkflowVersions() {
        return workflowVersion.collect { WorkflowVersion.get(it) }
    }

    static constraints = {
        group nullable: true
        name blank: false
        seqType nullable: false
        workflowVersion validator: { List<Long> val ->
            val && val.every { it != null && WorkflowVersion.exists(it) }
        }
    }
}

class DeleteWorkflowDefaultGroupCommand implements Validateable {
    WorkflowDefaultGroup group
}
