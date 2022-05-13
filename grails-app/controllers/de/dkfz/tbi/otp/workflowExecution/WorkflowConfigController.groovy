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
import groovy.util.logging.Slf4j
import org.hibernate.ObjectNotFoundException
import org.springframework.http.HttpStatus

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Slf4j
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

    ExternalWorkflowConfigSelectorService externalWorkflowConfigSelectorService

    JSON data() {
        ExternalWorkflowConfigSelectorSearchParameter searchParameter = new ExternalWorkflowConfigSelectorSearchParameter([
                workflowIds             : queryParameterToLongList("workflows", params),
                workflowVersionIds      : queryParameterToLongList("workflowVersions", params),
                projectIds              : queryParameterToLongList("projects", params),
                seqTypeIds              : queryParameterToLongList("seqTypes", params),
                referenceGenomeIds      : queryParameterToLongList("referenceGenomes", params),
                libraryPreparationKitIds: queryParameterToLongList("libraryPreparationKits", params),
                type                    : queryParameterToStringList("type", params),
        ])

        ExternalWorkflowConfigSelectorLists selectors = externalWorkflowConfigSelectorService.searchExternalWorkflowConfigSelectors(searchParameter)

        List<Map<String, Object>> selectorData = selectors.relatedSelectors.sort().collect { ExternalWorkflowConfigSelector selector ->
            transformToMap(selector, selectors.exactMatchSelectors)
        }

        render([data: selectorData] as JSON)
    }

    JSON fragments(Long id) {
        if (id == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "workflowConfig.backend.failed") as String)
        }
        try {
            ExternalWorkflowConfigSelector selector = externalWorkflowConfigSelectorService.getById(id)
            assert selector
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
            ExternalWorkflowConfigSelector selector = externalWorkflowConfigSelectorService.getById(id)
            assert selector
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
            ExternalWorkflowConfigSelector selector = externalWorkflowConfigSelectorService.findExactlyOneByExternalWorkflowConfigFragment(fragment)

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
                customPriority        : selector.customPriority,
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
        String key = "query[${parameterName}]"
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
