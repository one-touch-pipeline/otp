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
import grails.validation.Validateable
import org.springframework.security.access.annotation.Secured

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

    /**
     * Converter especially to get the correct time format in output data sets.
     *
     * @return map of workflow data
     */
    private Map buildWorkflowOutputObject(Workflow wf) {
        List<WorkflowVersion> versions = workflowVersionService.findAllByWorkflow(wf)
                .sort { a, b -> VersionComparator.COMPARATOR.compare(a.workflowVersion, b.workflowVersion) }

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
                defaultVersion      : wf.defaultVersion,
                versions            : versions,
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

class VersionComparator implements Comparator<String> {
    public static final Comparator<String> COMPARATOR = new VersionComparator()

    @Override
    int compare(String o1, String o2) {
        String[] s1 = split(o1)
        String[] s2 = split(o2)

        for (int i = 0; i < Math.min(s1.length, s2.length); i++) {
            int result = compareToken(s1[i], s2[i])
            if (result != 0) {
                return -result
            }
        }
        return 0
    }

    private split(String s) {
        return s.split(/[-\/.]/)
    }

    private int compareToken(String s1, String s2) {
        if (s1.isInteger() && s2.isInteger()) {
            return s1 as Integer <=> s2 as Integer
        }
        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2)
    }
}
