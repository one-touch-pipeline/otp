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

import grails.gorm.hibernate.annotation.ManagedEntity
import grails.util.Holders
import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

@ToString(includeNames = true, includePackage = false)
@ManagedEntity
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

    /**
     * This is a calculated property to represent the suggested priority of this selector.
     * Property priority is calculated and stored into database when the selector is created or modified, to optimise the reading speed.
     * <p>
     * Calculation is done with this method (@link #calculatePriority()), which ia called right before {#save()} is called
     */
    int priority

    void beforeInsert() {
        priority = calculatePriority()
    }

    void beforeUpdate() {
        priority = calculatePriority()
    }

    /**
     * Calculate the priority with the calculatePriority method from the according service
     */
    private int calculatePriority() {
        return ExternalWorkflowConfigSelectorService.calculatePriority([
                selectorType          : this.selectorType,
                projects              : this.projects,
                libraryPreparationKits: this.libraryPreparationKits,
                referenceGenomes      : this.referenceGenomes,
                seqTypes              : this.seqTypes,
                workflowVersions      : this.workflowVersions,
                workflows             : this.workflows,
        ] as CalculatePriorityDTO)
    }

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
        externalWorkflowConfigFragment validator: { val, obj ->
            List<String> duplicateKeys = validateUniqueKeys(val, obj)
            if (duplicateKeys) {
                return ["duplicate.key", duplicateKeys.join(", ")]
            }
        }
        workflowVersions validator: { val, obj ->
            if (val && !obj.workflows) {
                return "externalWorkflowConfigSelector.workflowVersions.workflowsEmpty"
            }
            WorkflowVersion currentVersion = val.find {
                !(it.workflow in obj.workflows)
            }
            if (currentVersion) {
                return ["externalWorkflowConfigSelector.workflowVersions.noWorkflowDefined", currentVersion]
            }
        }
    }

    static Closure mapping = {
        externalWorkflowConfigFragment index: "external_workflow_config_selector_external_workflow_config_fragment_idx"
        selectorType index: "external_workflow_config_selector_selector_type_idx"
    }

    @Override
    int compareTo(ExternalWorkflowConfigSelector externalWorkflowConfigSelector) {
        return externalWorkflowConfigSelector.priority <=> priority
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

    static List<String> validateUniqueKeys(ExternalWorkflowConfigFragment fragment, ExternalWorkflowConfigSelector selector) {
        ConfigSelectorService configSelectorService = Holders.applicationContext.getBean(ConfigSelectorService)

        List<ExternalWorkflowConfigSelector> otherSelectors = configSelectorService.findExactSelectors(new MultiSelectSelectorExtendedCriteria(
                selector.workflows,
                selector.workflowVersions,
                selector.projects,
                selector.seqTypes,
                selector.referenceGenomes,
                selector.libraryPreparationKits,
        )) - selector

        List<String> duplicatedKeys = []
        if (otherSelectors) {
            List<List<String>> keys = []
            keysToList(fragment.configValuesToMap(), keys)
            otherSelectors.each { otherSelector ->
                List<List<String>> otherKeys = []
                keysToList(otherSelector.externalWorkflowConfigFragment.configValuesToMap(), otherKeys)
                keys.each {
                    if (it in otherKeys) {
                        duplicatedKeys.add(it.join("."))
                    }
                }
            }
        }
        return duplicatedKeys
    }

    private static void keysToList(Map conf, List<List<String>> keys, List<String> key = []) {
        conf.entrySet().each { Map.Entry entry ->
            List<String> currentKey = key + [entry.key as String]
            if (entry.value instanceof Map) {
                keysToList(entry.value as Map, keys, currentKey)
            } else {
                keys.add(currentKey)
            }
        }
    }
}
