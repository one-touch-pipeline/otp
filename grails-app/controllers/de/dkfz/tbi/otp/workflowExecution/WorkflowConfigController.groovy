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
package de.dkfz.tbi.otp.workflowExecution

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import groovy.transform.TupleConstructor
import org.hibernate.ObjectNotFoundException
import org.springframework.http.HttpStatus

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@Secured("hasRole('ROLE_ADMIN')")
class WorkflowConfigController implements BaseWorkflowConfigController {

    static allowedMethods = [
            index    : "GET",
            create   : "POST",
            update   : "POST",
            deprecate: "POST",
            data     : "GET",
            selector : "GET",
            fragments: "GET",
    ]

    ConfigSelectorService configSelectorService

    JSON data() {
        Map searchQuery = [:]
        params.findAll { it.key.startsWith('query') }.each { String key, String value ->
            String mapKey = key.replace('query[', '').replace(']', '')
            searchQuery[mapKey] = value ? value.split(',') : null
        }
        log.debug("searchQuery: ${searchQuery}")

        List<ExternalWorkflowConfigSelector> relatedSelectors, exactlyMatchingSelectors
        if (searchQuery) {
            Set<Workflow> selectedWorkflows = searchQuery['workflows'].collect {
                CollectionUtils.exactlyOneElement(Workflow.findAllById(it))
            } as Set
            Set<WorkflowVersion> selectedWorkflowVersions = searchQuery['workflowVersions'].collect {
                CollectionUtils.exactlyOneElement(WorkflowVersion.findAllById(it))
            } as Set
            Set<Project> selectedProjects = searchQuery['projects'].collect {
                CollectionUtils.exactlyOneElement(Project.findAllById(it))
            } as Set
            Set<SeqType> selectedSeqTypes = searchQuery['seqTypes'].collect {
                CollectionUtils.exactlyOneElement(SeqType.findAllById(it))
            } as Set
            Set<ReferenceGenome> selectedReferenceGenomes = searchQuery['referenceGenomes'].collect {
                CollectionUtils.exactlyOneElement(ReferenceGenome.findAllById(it))
            } as Set
            Set<LibraryPreparationKit> selectedLibraryPreparationKits = searchQuery['libraryPreparationKits'].collect {
                CollectionUtils.exactlyOneElement(LibraryPreparationKit.findAllById(it))
            } as Set

            MultiSelectSelectorExtendedCriteria criteria = new MultiSelectSelectorExtendedCriteria(
                    selectedWorkflows,
                    selectedWorkflowVersions,
                    selectedProjects,
                    selectedSeqTypes,
                    selectedReferenceGenomes,
                    selectedLibraryPreparationKits
            )

            exactlyMatchingSelectors = configSelectorService.findExactSelectors(criteria)
            log.debug("Exact matches: ${exactlyMatchingSelectors.size()}")

            relatedSelectors = configSelectorService.findAllRelatedSelectors(criteria).sort()
            if (searchQuery['type']) {
                relatedSelectors = relatedSelectors.findAll { ExternalWorkflowConfigSelector sel ->
                    searchQuery['type'].any {
                        it == sel.selectorType.toString()
                    }
                }
            }

            log.debug("Related matches: ${relatedSelectors.size()}")
        } else {
            relatedSelectors = ExternalWorkflowConfigSelector.findAllBySelectorType(
                    SelectorType.DEFAULT_VALUES
            ).sort()
            log.debug("index: ${relatedSelectors.size()} selectors found")
        }

        List<Map<String, Object>> selectorData = relatedSelectors.collect { ExternalWorkflowConfigSelector selector ->
            transformToMap(selector, exactlyMatchingSelectors)
        }

        render([data: selectorData] as JSON)
    }

    JSON fragments(Long id) {
        if (id == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "workflowConfig.backend.failed") as String)
        }
        try {
            ExternalWorkflowConfigSelector selector = CollectionUtils.exactlyOneElement(ExternalWorkflowConfigSelector.findAllById(id))
            render selector?.fragments as JSON
        } catch (ObjectNotFoundException ex) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "${g.message(code: "workflowConfig.backend.failed")}: ${ex.message}")
        } catch (AssertionError ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "${g.message(code: "workflowConfig.backend.failed")}: ${ex.message}")
        }
    }

    JSON selector(Long id) {
        if (id == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "workflowConfig.backend.failed") as String)
        }
        try {
            ExternalWorkflowConfigSelector selector = CollectionUtils.exactlyOneElement(ExternalWorkflowConfigSelector.findAllById(id))
            render transformToMap(selector) as JSON
        } catch (ObjectNotFoundException ex) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "${g.message(code: "workflowConfig.backend.failed")}: ${ex.message}")
        } catch (AssertionError ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "${g.message(code: "workflowConfig.backend.failed")}: ${ex.message}")
        }
    }

    def index() {
        List<ExternalWorkflowConfigSelector> allSelectors = configSelectorService.all

        return [
                allSelectors            : allSelectors,
                selectorTypes           : SelectorType.values(),
                columns                 : Column.values(),

                workflows               : workflows,
                workflowVersions        : workflowVersions,
                projects                : projects,
                seqTypes                : seqTypes,
                referenceGenomes        : referenceGenomes,
                libraryPreparationKits  : libraryPreparationKits,
        ]
    }

    JSON create(CreateCommand cmd) {
        log.debug("selector creating: ${cmd.selectorName}")
        checkErrorAndCallMethodReturns(cmd) {
            ExternalWorkflowConfigSelector selector = configSelectorService.create(cmd)

            render transformToMap(selector) as JSON
        }
    }

    JSON update(UpdateCommand cmd) {
        log.debug("selector updating: ${cmd.selector.name}")
        checkErrorAndCallMethodReturns(cmd) {
            ExternalWorkflowConfigSelector selector = configSelectorService.update(cmd)

            render transformToMap(selector) as JSON
        }
    }

    JSON deprecate(ExternalWorkflowConfigFragment fragment) {
        log.debug("selector deprecating: ${fragment.name}")
        try {
            ExternalWorkflowConfigSelector selector = CollectionUtils.exactlyOneElement(
                    ExternalWorkflowConfigSelector.findAllByExternalWorkflowConfigFragment(fragment))

            Map<String, Object> deprecatedSelector = transformToMap(selector)
            configSelectorService.deprecate(fragment)

            render deprecatedSelector as JSON
        } catch (AssertionError ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "${g.message(code: "workflowConfig.deprecated.failed")}: ${ex.message}")
        }
    }

    private long fetchFragmentId(ExternalWorkflowConfigSelector selector) {
        try {
            return selector.externalWorkflowConfigFragment.id
        } catch (ObjectNotFoundException ex) {
            return  -1
        }
    }

    private Map<String, Object> transformToMap(ExternalWorkflowConfigSelector selector, List<ExternalWorkflowConfigSelector> exactlyMatchingSelectors = null) {
        return [
                id: selector.id,
                fid: fetchFragmentId(selector),
                exactMatch: exactlyMatchingSelectors ? exactlyMatchingSelectors.any {
                    it.id == selector.id
                } : false,
                name: selector.name,
                selectorType: selector.selectorType,
                customPriority: selector.customPriority,
                workflows: selector.workflows.collect {
                    [
                            id: it.id,
                            name: it.displayName
                    ]
                },
                workflowVersions: selector.workflowVersions.collect {
                    [
                            id: it.id,
                            name: it.displayName
                    ]
                },
                projects: selector.projects.collect {
                    [
                            id: it.id,
                            name: it.displayName
                    ]
                },
                seqTypes: selector.seqTypes.collect {
                    [
                            id: it.id,
                            name: it.displayNameWithLibraryLayout
                    ]
                },
                referenceGenomes: selector.referenceGenomes.collect {
                    [
                            id: it.id,
                            name: it.name
                    ]
                },
                libraryPreparationKits: selector.libraryPreparationKits.collect {
                    [
                            id: it.id,
                            name: it.name
                    ]
                },
        ]
    }

    @TupleConstructor
    enum Column {
        ID("workflowConfig.selector.id", "id", "Hidden"),
        FID("", "fid", "Hidden"),
        EXACT_MATCH("", "exactMatch", "Boolean"),
        SELECTOR_NAME("workflowConfig.selector.name", "selectorName", "Text"),
        SELECTOR_TYPE("workflowConfig.selector.type", "selectorType", "Text"),
        CUSTOM_PRIORITY("workflowConfig.selector.customPriority", "customPriority", "Number"),
        WORKFLOWS("workflowConfig.selector.workflows", "workflows", "MultiSelect"),
        WORKFLOW_VERSIONS("workflowConfig.selector.versions", "workflowVersions", "MultiSelect"),
        PROJECTS("workflowConfig.selector.projects", "projects", "MultiSelect"),
        SEQ_TYPES("workflowConfig.selector.seqTypes", "seqTypes", "MultiSelect"),
        REFERENCE_GENOMES("workflowConfig.selector.referenceGenomes", "referenceGenomes", "MultiSelect"),
        LIBRARY_PREPARATION_KITS("workflowConfig.selector.libPrepKits", "libraryPreparationKits", "MultiSelect"),
        BUTTONS("", "", ""),

        final String message
        final String orderColumn
        final String renderer
    }
}

class CreateCommand extends SelectorCommand {
    String selectorName
    SelectorType type
    String fragmentName
    Integer customPriority
    String value

    Set<ExternalWorkflowConfigSelector> matchingSelectors
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
