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

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.util.TimeFormats
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable

import java.time.LocalDate

@Secured("hasRole('ROLE_ADMIN')")
class WorkflowSystemConfigController implements CheckAndCall {

    static allowedMethods = [
            index         : "GET",
            getWorkflows  : "GET",
            updateWorkflow: "POST",
    ]

    def index() {
        return [
                refGenomes: ReferenceGenome.findAll().sort { it.name },
                seqTypes  : SeqType.findAll().sort { it.displayNameWithLibraryLayout },
        ]
    }

    def getWorkflows() {
        List<Map> workflows = Workflow.findAll().collect { Workflow wf ->
            buildWorkflowOutputObject(wf)
        }
        render workflows as JSON
    }

    def updateWorkflow(WorkflowUpdateCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            Workflow workflow = Workflow.findById(cmd.id)
            workflow.priority = cmd.priority
            workflow.enabled = cmd.enabled
            workflow.maxParallelWorkflows = cmd.maxParallelWorkflows

            if (cmd.supportedSeqTypes) {
                workflow.supportedSeqTypes = SeqType.findAll { id in cmd.supportedSeqTypes }
            } else {
                workflow.supportedSeqTypes = null
            }

            if (cmd.allowedRefGenomes) {
                workflow.allowedReferenceGenomes = ReferenceGenome.findAll {
                    id in cmd.allowedRefGenomes
                }
            } else {
                workflow.allowedReferenceGenomes = null
            }

            if (cmd.deprecated && !workflow.deprecatedDate) {
                workflow.deprecatedDate = LocalDate.now()
            } else if (!cmd.deprecated) {
                workflow.deprecatedDate = null
            }

            workflow.save(flush: true)
            Map output = buildWorkflowOutputObject(workflow)
            render output as JSON
        }
    }

    /**
     * Converter especially to get the correct time format in output data sets.
     *
     * @param wf
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

class WorkflowUpdateCommand implements Validateable {
    int id
    short priority
    short maxParallelWorkflows
    boolean enabled
    boolean deprecated
    List<Long> allowedRefGenomes
    List<Long> supportedSeqTypes
}
