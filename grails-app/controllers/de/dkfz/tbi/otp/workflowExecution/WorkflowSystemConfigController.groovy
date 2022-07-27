/*
 * Copyright 2011-2021 The OTP authors
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

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.util.TimeFormats

@Secured("hasRole('ROLE_ADMIN')")
class WorkflowSystemConfigController implements CheckAndCall {

    static allowedMethods = [
            index         : "GET",
            getWorkflows  : "GET",
            updateWorkflow: "POST",
    ]

    MergingCriteriaService mergingCriteriaService

    ReferenceGenomeService referenceGenomeService

    SeqTypeService seqTypeService

    WorkflowService workflowService

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
        render workflows as JSON
    }

    def updateWorkflow(WorkflowUpdateCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            UpdateWorkflowDto updateWorkflowDto = new UpdateWorkflowDto(
                    cmd.id,
                    cmd.priority,
                    cmd.maxParallelWorkflows,
                    cmd.enabled,
                    cmd.allowedRefGenomes,
                    cmd.supportedSeqTypes,
            )

            Workflow workflow = workflowService.updateWorkflow(updateWorkflowDto)
            Map output = buildWorkflowOutputObject(workflow)
            render output as JSON
        }
    }

    /**
     * Converter especially to get the correct time format in output data sets.
     *
     * @return map of workflow data
     */
    private static Map buildWorkflowOutputObject(Workflow wf) {
        List<Map> supportedSeqTypes = wf.supportedSeqTypes.collect { SeqType st ->
            [
                    id           : st.id,
                    displayName  : st.displayName,
                    libraryLayout: st.libraryLayout,
                    singleCell   : st.singleCell,
            ]
        }

        List<Map> allowedRefGenomes = wf.allowedReferenceGenomes.collect { ReferenceGenome rg ->
            [
                    id  : rg.id,
                    name: rg.name,
            ]
        }

        return [
                id                  : wf.id,
                name                : wf.name,
                priority            : wf.priority,
                enabled             : wf.enabled,
                maxParallelWorkflows: wf.maxParallelWorkflows,
                deprecationDate     : TimeFormats.DATE.getFormattedLocalDate(wf.deprecatedDate),
                supportedSeqTypes   : supportedSeqTypes,
                allowedRefGenomes   : allowedRefGenomes,
        ]
    }
}

class WorkflowUpdateCommand extends UpdateWorkflowDto implements Validateable {
}
