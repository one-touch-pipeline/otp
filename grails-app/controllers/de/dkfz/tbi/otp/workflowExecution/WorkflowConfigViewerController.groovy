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

import com.fasterxml.jackson.core.JsonParseException
import grails.converters.JSON
import grails.validation.Validateable
import groovy.transform.ToString
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@PreAuthorize("hasRole('ROLE_ADMIN')")
class WorkflowConfigViewerController implements BaseWorkflowConfigController {

    ConfigSelectorService configSelectorService
    ConfigFragmentService configFragmentService
    ProcessingOptionService processingOptionService

    static allowedMethods = [
            index: 'GET',
            build: 'POST',
    ]

    def index() {
        return [
                workflows             : workflows,
                workflowVersions      : workflowVersions,
                projects              : projects,
                seqTypes              : seqTypes,
                referenceGenomes      : referenceGenomes,
                libraryPreparationKits: libraryPreparationKits,
                selectorTypes         : selectorTypes,
        ]
    }

    @SuppressWarnings('UnnecessarySetter')
    def build(SingleSelectorCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), cmd.errors.toString())
        }
        SingleSelectSelectorExtendedCriteria extendedCriteria
        extendedCriteria = new SingleSelectSelectorExtendedCriteria(
                cmd.workflow,
                cmd.workflowVersion,
                cmd.selectedProject,
                cmd.seqType,
                cmd.referenceGenome,
                cmd.libraryPreparationKit,
        )

        List<ExternalWorkflowConfigSelector> selectors = configSelectorService.findAllSelectorsSortedByPriority(extendedCriteria)
        List<ExternalWorkflowConfigFragment> fragments = selectors*.externalWorkflowConfigFragment

        response.setContentType(MediaType.APPLICATION_JSON_VALUE)

        try {
            String fragmentJson = configFragmentService.mergeSortedFragments(fragments)
            Map data = [
                    selectors: g.render(template: '/templates/bootstrap_selector', model: [relatedSelectors: selectors]),
                    config   : fragmentJson,
            ]
            render(data as JSON)
        } catch (JsonParseException ignored) {
            response.sendError(HttpStatus.BAD_REQUEST.value(),
                    g.message(code: 'workflowConfigViewer.json.parsing.error') as String)
        }
    }

}

@ToString
class SingleSelectorCommand implements Validateable {

    Workflow workflow
    WorkflowVersion workflowVersion
    Project selectedProject
    SeqType seqType
    ReferenceGenome referenceGenome
    LibraryPreparationKit libraryPreparationKit

    static constraints = {
        workflow(nullable: true)
        workflowVersion(nullable: true)
        selectedProject(nullable: true)
        seqType(nullable: true)
        referenceGenome(nullable: true)
        libraryPreparationKit(nullable: true)
    }

}
