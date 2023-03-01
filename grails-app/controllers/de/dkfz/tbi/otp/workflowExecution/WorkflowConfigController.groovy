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
import grails.validation.Validateable
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.hibernate.ObjectNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Slf4j
@PreAuthorize("hasRole('ROLE_ADMIN')")
class WorkflowConfigController implements BaseWorkflowConfigController {

    static allowedMethods = [
            index    : "GET",
            create   : "POST",
            check    : "POST",
            update   : "POST",
            deprecate: "POST",
            data     : "GET",
            selector : "GET",
            fragments: "GET",
    ]

    ConfigSelectorService configSelectorService

    ExternalWorkflowConfigSelectorService externalWorkflowConfigSelectorService

    JSON data() {
        ExternalWorkflowConfigSelectorSearchParameter searchParameter = new ExternalWorkflowConfigSelectorSearchParameter([
                workflowIds             : queryParameterToLongList("query[workflows]", params as Map),
                workflowVersionIds      : queryParameterToLongList("query[workflowVersions]", params as Map),
                projectIds              : queryParameterToLongList("query[projects]", params as Map),
                seqTypeIds              : queryParameterToLongList("query[seqTypes]", params as Map),
                referenceGenomeIds      : queryParameterToLongList("query[referenceGenomes]", params as Map),
                libraryPreparationKitIds: queryParameterToLongList("query[libraryPreparationKits]", params as Map),
                type                    : queryParameterToStringList("query[type]", params as Map),
        ])

        ExternalWorkflowConfigSelectorLists selectors = externalWorkflowConfigSelectorService.searchExternalWorkflowConfigSelectors(searchParameter)

        List<Map<String, Object>> selectorData = selectors.relatedSelectors.sort().collect { ExternalWorkflowConfigSelector selector ->
            transformToMap(selector, selectors.exactMatchSelectors)
        }

        return render([data: selectorData] as JSON)
    }

    JSON fragments(Long id) {
        if (id == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "workflowConfig.backend.failed") as String)
        }
        try {
            ExternalWorkflowConfigSelector selector = externalWorkflowConfigSelectorService.getById(id)
            assert selector
            render(selector?.fragments as JSON)
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
            ExternalWorkflowConfigSelector selector = externalWorkflowConfigSelectorService.getById(id)
            assert selector
            render(transformToMap(selector) as JSON)
        } catch (ObjectNotFoundException ex) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "${g.message(code: "workflowConfig.backend.failed")}: ${ex.message}")
        } catch (AssertionError ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "${g.message(code: "workflowConfig.backend.failed")}: ${ex.message}")
        }
    }

    JSON check(CheckCommand cmd) {
        int calculatedPriority = ExternalWorkflowConfigSelectorService.calculatePriority([
                selectorType          : cmd.type,
                projects              : cmd.projects,
                libraryPreparationKits: cmd.libraryPreparationKits,
                referenceGenomes      : cmd.referenceGenomes,
                seqTypes              : cmd.seqTypes,
                workflowVersions      : cmd.workflowVersions,
                workflows             : cmd.workflows,
        ] as CalculatePriorityDTO)

        OverwritingSubstitutedAndConflictingSelectors groupedSelectors =
                externalWorkflowConfigSelectorService.getOverwritingSubstitutedAndConflictingSelectors(
                        cmd.workflows*.id, cmd.workflowVersions*.id, cmd.projects*.id, cmd.referenceGenomes*.id,
                        cmd.seqTypes*.id, cmd.libraryPreparationKits*.id, calculatedPriority
                )

        groupedSelectors.allSelectors.each { selector ->
            selector.conflictingParameters = externalWorkflowConfigSelectorService
                    .getAllConflictingConfigValues(cmd.fragmentValue, selector.configFragmentValue)
        }

        def overwritingSelectors = groupedSelectors.overwritingSelectors.findAll { selector ->
            return selector.conflictingParameters.size() > 0
        }.collect { selector ->
            [name: selector.selectorName, projectNames: selector.projectNames, conflictingParameters: selector.conflictingParameters]
        }

        def substitutedSelectors = groupedSelectors.substitutedSelectors.findAll { selector ->
            return selector.conflictingParameters.size() > 0
        }.collect { selector ->
            [name: selector.selectorName, projectNames: selector.projectNames, conflictingParameters: selector.conflictingParameters]
        }

        def conflictingSelectors = groupedSelectors.conflictingSelectors.findAll { selector ->
            return selector.conflictingParameters.size() > 0
        }.collect { selector ->
            [name: selector.selectorName, projectNames: selector.projectNames, conflictingParameters: selector.conflictingParameters]
        }

        return render([
                overwritingSelectors : overwritingSelectors,
                substitutedSelectors : substitutedSelectors,
                conflictingSelectors : conflictingSelectors,
        ] as JSON)
    }

    def index() {
        List<ExternalWorkflowConfigSelector> allSelectors = configSelectorService.all

        return [
                allSelectors          : allSelectors,
                selectorTypes         : SelectorType.values(),
                columns               : Column.values(),

                workflows             : workflows,
                workflowVersions      : workflowVersions,
                projects              : projects,
                seqTypes              : seqTypes,
                referenceGenomes      : referenceGenomes,
                libraryPreparationKits: libraryPreparationKits,
        ]
    }

    JSON create(CreateCommand cmd) {
        log.debug("selector creating: ${cmd.selectorName}")
        checkErrorAndCallMethodReturns(cmd) {
            ExternalWorkflowConfigSelector selector = configSelectorService.create(cmd)

            return render(transformToMap(selector) as JSON)
        } as JSON
    }

    JSON update(UpdateCommand cmd) {
        log.debug("selector updating: ${cmd.selector.name}")
        checkErrorAndCallMethodReturns(cmd) {
            ExternalWorkflowConfigSelector selector = configSelectorService.update(cmd)

            return render(transformToMap(selector) as JSON)
        } as JSON
    }

    JSON deprecate(ExternalWorkflowConfigSelector selector) {
        log.debug("selector deprecating: ${selector.name}")
        try {
            Map<String, Object> deprecatedSelector = transformToMap(selector)
            configSelectorService.deprecate(selector)

            render(deprecatedSelector as JSON)
        } catch (AssertionError ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "${g.message(code: "workflowConfig.deprecated.failed")}: ${ex.message}")
        }
    }

    private long fetchFragmentId(ExternalWorkflowConfigSelector selector) {
        try {
            return selector.externalWorkflowConfigFragment.id
        } catch (ObjectNotFoundException ex) {
            return -1
        }
    }

    private Map<String, Object> transformToMap(ExternalWorkflowConfigSelector selector, List<ExternalWorkflowConfigSelector> exactlyMatchingSelectors = null) {
        return [
                id                    : selector.id,
                fid                   : fetchFragmentId(selector),
                exactMatch            : exactlyMatchingSelectors ? exactlyMatchingSelectors.any {
                    it.id == selector.id
                } : false,
                name                  : selector.name,
                selectorType          : selector.selectorType,
                priority              : selector.priority,
                workflows             : selector.workflows.collect {
                    [
                            id  : it.id,
                            name: it.displayName
                    ]
                },
                workflowVersions      : selector.workflowVersions.collect {
                    [
                            id  : it.id,
                            name: it.displayName
                    ]
                },
                projects              : selector.projects.collect {
                    [
                            id  : it.id,
                            name: it.displayName
                    ]
                },
                seqTypes              : selector.seqTypes.collect {
                    [
                            id  : it.id,
                            name: it.displayNameWithLibraryLayout
                    ]
                },
                referenceGenomes      : selector.referenceGenomes.collect {
                    [
                            id  : it.id,
                            name: it.name
                    ]
                },
                libraryPreparationKits: selector.libraryPreparationKits.collect {
                    [
                            id  : it.id,
                            name: it.name
                    ]
                },
        ]
    }

    private List<String> queryParameterToStringList(String parameterName, Map params) {
        String key = "${parameterName}"
        String value = params[key]
        return value ? value.split(',')*.trim() : []
    }

    private List<Long> queryParameterToLongList(String parameterName, Map params) {
        return queryParameterToStringList(parameterName, params)*.toLong()
    }

    @TupleConstructor
    enum Column {
        ID("workflowConfig.selector.id", "id", "Hidden"),
        FID("", "fid", "Hidden"),
        EXACT_MATCH("", "exactMatch", "Boolean"),
        SELECTOR_NAME("workflowConfig.selector.name", "selectorName", "Text"),
        SELECTOR_TYPE("workflowConfig.selector.type", "selectorType", "Text"),
        PRIORITY("workflowConfig.selector.priority", "priority", "Number"),
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
    String value

    Set<ExternalWorkflowConfigSelector> matchingSelectors
}

class CheckCommand extends SelectorCommand {
    String fragmentValue
    SelectorType type
}

class UpdateCommand extends CreateCommand {
    ExternalWorkflowConfigSelector selector
}

class SelectorCommand implements Validateable {
    List<Workflow> workflows
    List<WorkflowVersion> workflowVersions
    List<Project> projects
    List<SeqType> seqTypes
    List<ReferenceGenome> referenceGenomes
    List<LibraryPreparationKit> libraryPreparationKits
}
