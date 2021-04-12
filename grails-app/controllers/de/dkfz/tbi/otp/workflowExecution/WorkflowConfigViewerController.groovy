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

import com.fasterxml.jackson.core.JsonParseException
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project

@Secured("hasRole('ROLE_ADMIN')")
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
                cmd.project,
                cmd.seqType,
                cmd.referenceGenome,
                cmd.libraryPreparationKit,
        )
        List<ExternalWorkflowConfigSelector> selectors = configSelectorService.findAllSelectors(extendedCriteria)
        List<ExternalWorkflowConfigFragment> fragments = selectors*.externalWorkflowConfigFragment

        response.setContentType(MediaType.APPLICATION_JSON_VALUE)

        try {
            String fragmentJson = configFragmentService.mergeSortedFragments(fragments) as JSON
            Map data = [
                    selectors: g.render(template: '/templates/bootstrap_selector', model: [relatedSelectors: selectors]),
                    config   : fragmentJson,
            ]
            render data as JSON
        } catch (JsonParseException ignored) {
            response.sendError(HttpStatus.BAD_REQUEST.value(),
                    g.message(code: 'workflowConfigViewer.json.parsing.error') as String)
        }
    }

}

class SingleSelectorCommand implements Validateable {

    Workflow workflow
    WorkflowVersion workflowVersion
    Project project
    SeqType seqType
    ReferenceGenome referenceGenome
    LibraryPreparationKit libraryPreparationKit

    static constraints = {
        workflow(nullable: true)
        workflowVersion(nullable: true)
        project(nullable: true)
        seqType(nullable: true)
        referenceGenome(nullable: true)
        libraryPreparationKit(nullable: true)
    }

}
