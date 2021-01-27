package de.dkfz.tbi.otp.workflowExecution

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

import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Secured(['ROLE_ADMIN'])
class WorkflowConfigController implements BaseWorkflowConfigController {

    static allowedMethods = [
            index    : "GET",
            create   : "POST",
            update   : "POST",
            deprecate: "POST",
    ]

    ConfigSelectorService configSelectorService

    @SuppressWarnings(["AbcMetric", "CyclomaticComplexity"])
    def index(ShowCommand cmd) {
        ExternalWorkflowConfigSelector selector
        ExternalWorkflowConfigFragment fragment
        if (cmd.fragment) {
            selector = cmd.fragment.selector.orElse(null)
            fragment = cmd.fragment
        } else if (cmd.selector) {
            selector = cmd.selector
            fragment = selector.externalWorkflowConfigFragment
        } else {
            MultiSelectSelectorExtendedCriteria extendedCriteria = new MultiSelectSelectorExtendedCriteria(
                    cmd.workflows as Set,
                    cmd.workflowVersions as Set,
                    cmd.projects as Set,
                    cmd.seqTypes as Set,
                    cmd.referenceGenomes as Set,
                    cmd.libraryPreparationKits as Set
            )
            selector = configSelectorService.findExactSelector(extendedCriteria)
            fragment = selector?.externalWorkflowConfigFragment
        }

        Set<Workflow> selectedWorkflows = (selector ? selector.workflows : cmd.workflows) as Set
        Set<WorkflowVersion> selectedWorkflowVersions = (selector ? selector.workflowVersions : cmd.workflowVersions) as Set
        Set<Project> selectedProjects = (selector ? selector.projects : cmd.projects) as Set
        Set<SeqType> selectedSeqTypes = (selector ? selector.seqTypes : cmd.seqTypes) as Set
        Set<ReferenceGenome> selectedReferenceGenomes = (selector ? selector.referenceGenomes : cmd.referenceGenomes) as Set
        Set<LibraryPreparationKit> selectedLibraryPreparationKits = (selector ? selector.libraryPreparationKits : cmd.libraryPreparationKits) as Set

        CreateCommand editedCmd = flash.cmd as CreateCommand

        Set<Workflow> editedWorkflows = (editedCmd ? editedCmd.workflows : selector ? selector.workflows : cmd.workflows) as Set
        Set<WorkflowVersion> editedWorkflowVersions = (editedCmd ? editedCmd.workflowVersions :
                selector ? selector.workflowVersions : cmd.workflowVersions) as Set
        Set<Project> editedProjects = (editedCmd ? editedCmd.projects : selector ? selector.projects : cmd.projects) as Set
        Set<SeqType> editedSeqTypes = (editedCmd ? editedCmd.seqTypes : selector ? selector.seqTypes : cmd.seqTypes) as Set
        Set<ReferenceGenome> editedReferenceGenomes = (editedCmd ? editedCmd.referenceGenomes :
                selector ? selector.referenceGenomes : cmd.referenceGenomes) as Set
        Set<LibraryPreparationKit> editedLibraryPreparationKits = (editedCmd ? editedCmd.libraryPreparationKits :
                selector ? selector.libraryPreparationKits : cmd.libraryPreparationKits) as Set

        List<ExternalWorkflowConfigSelector> relatedSelectors = configSelectorService.findAllRelatedSelectors(new MultiSelectSelectorExtendedCriteria(
                selectedWorkflows,
                selectedWorkflowVersions,
                selectedProjects,
                selectedSeqTypes,
                selectedReferenceGenomes,
                selectedLibraryPreparationKits
        )).sort()

        List<ExternalWorkflowConfigSelector> allSelectors = configSelectorService.all

        return [
                allSelectors                  : allSelectors,

                selector                      : selector,
                fragment                      : fragment,
                fragments                     : selector?.fragments ?: [],
                relatedSelectors              : relatedSelectors,

                workflows                     : workflows,
                workflowVersions              : workflowVersions,
                projects                      : projects,
                seqTypes                      : seqTypes,
                referenceGenomes              : referenceGenomes,
                libraryPreparationKits        : libraryPreparationKits,

                selectedWorkflows             : selectedWorkflows,
                selectedWorkflowVersions      : selectedWorkflowVersions,
                selectedProjects              : selectedProjects,
                selectedSeqTypes              : selectedSeqTypes,
                selectedReferenceGenomes      : selectedReferenceGenomes,
                selectedLibraryPreparationKits: selectedLibraryPreparationKits,

                editedWorkflows               : editedWorkflows,
                editedWorkflowVersions        : editedWorkflowVersions,
                editedProjects                : editedProjects,
                editedSeqTypes                : editedSeqTypes,
                editedReferenceGenomes        : editedReferenceGenomes,
                editedLibraryPreparationKits  : editedLibraryPreparationKits,
                editedCmd                     : editedCmd,

                selectorTypes                 : selectorTypes,
                cmd                           : cmd,
        ]
    }

    def create(CreateCommand cmd) {
        ExternalWorkflowConfigSelector selector = checkErrorAndCallMethodWithFlashMessage(cmd, "workflowConfig.created") {
            configSelectorService.create(cmd)
        }
        if (selector) {
            redirect action: "index", params: ["selector.id": selector.id]
        } else {
            redirect action: "index"
        }
    }

    def update(UpdateCommand cmd) {
        ExternalWorkflowConfigSelector selector = checkErrorAndCallMethodWithFlashMessage(cmd, "workflowConfig.updated") {
            configSelectorService.update(cmd)
        }
        if (selector) {
            redirect action: "index", params: ["selector.id": selector.id]
        } else {
            redirect action: "index", params: ["fragment.id": cmd.fragment.id]
        }
    }

    def deprecate(ExternalWorkflowConfigFragment fragment) {
        checkErrorAndCallMethodWithFlashMessage(null, "workflowConfig.deprecated") {
            configSelectorService.deprecate(fragment)
        }
        redirect action: "index", params: ["fragment.id": fragment.id]
    }
}

class ShowCommand extends SelectorCommand {
    ExternalWorkflowConfigSelector selector
    ExternalWorkflowConfigFragment fragment
}

class CreateCommand extends SelectorCommand {
    String selectorName
    SelectorType type
    String fragmentName
    Integer customPriority
    String value
}

class UpdateCommand extends CreateCommand {
    ExternalWorkflowConfigSelector selector
    ExternalWorkflowConfigFragment fragment
}

class SelectorCommand implements Validateable {
    List<Workflow> workflows
    List<WorkflowVersion> workflowVersions
    List<Project> projects
    List<SeqType> seqTypes
    List<ReferenceGenome> referenceGenomes
    List<LibraryPreparationKit> libraryPreparationKits
}
