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

import grails.util.Holders
import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

@ToString(includeNames = true, includePackage = false)
class ExternalWorkflowConfigSelector implements Comparable<ExternalWorkflowConfigSelector>, Entity {

    String name

    Set<Workflow> workflows
    Set<WorkflowVersion> workflowVersions
    Set<Project> projects
    Set<SeqType> seqTypes
    Set<ReferenceGenome> referenceGenomes
    Set<LibraryPreparationKit> libraryPreparationKits

    ExternalWorkflowConfigFragment externalWorkflowConfigFragment
    SelectorType selectorType
    int customPriority

    static hasMany = [
            workflows             : Workflow,
            workflowVersions      : WorkflowVersion,
            projects              : Project,
            seqTypes              : SeqType,
            referenceGenomes      : ReferenceGenome,
            libraryPreparationKits: LibraryPreparationKit,
    ]

    static constraints = {
        name unique: true, blank: false, validator: { val, obj ->
            if (obj.selectorType == SelectorType.DEFAULT_VALUES && !val.startsWith("Default values")) {
                return "default"
            }
            if (obj.selectorType != SelectorType.DEFAULT_VALUES && val.startsWith("Default values")) {
                return "no.default"
            }
        }
        selectorType validator: { val, obj ->
            if (val == SelectorType.DEFAULT_VALUES && (
                    obj.projects ||
                            obj.referenceGenomes ||
                            obj.libraryPreparationKits)) {
                return "default"
            }
        }
        customPriority min: 0, validator: { val, obj ->
            if (obj.selectorType == SelectorType.DEFAULT_VALUES && val != 0) {
                return "default"
            }
            if (obj.selectorType != SelectorType.DEFAULT_VALUES && val == 0) {
                return "min"
            }
        }
    }

    int calculateSuggestedPriority() {
        return 0
    }

    @Override
    int compareTo(ExternalWorkflowConfigSelector externalWorkflowConfigSelector) {
        return externalWorkflowConfigSelector.customPriority <=> customPriority
    }

    List<ExternalWorkflowConfigFragment> getFragments() {
        List<ExternalWorkflowConfigFragment> result = []
        ExternalWorkflowConfigFragment f = externalWorkflowConfigFragment
        while (f) {
            result.add(f)
            f = f.previous
        }
        return result
    }

    static boolean validateIsUnique(ExternalWorkflowConfigSelector externalWorkflowConfigSelector) {
        ExternalWorkflowConfigSelector other = Holders.applicationContext.getBean(ConfigSelectorService).findExactSelector(
                new MultiSelectSelectorExtendedCriteria(
                        externalWorkflowConfigSelector.workflows,
                        externalWorkflowConfigSelector.workflowVersions,
                        externalWorkflowConfigSelector.projects,
                        externalWorkflowConfigSelector.seqTypes,
                        externalWorkflowConfigSelector.referenceGenomes,
                        externalWorkflowConfigSelector.libraryPreparationKits,
                )
        )
        return !(other && other.id != externalWorkflowConfigSelector.id)
    }
}
