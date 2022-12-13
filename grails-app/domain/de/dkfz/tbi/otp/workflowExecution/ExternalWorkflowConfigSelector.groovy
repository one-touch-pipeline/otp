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

    def beforeInsert() {
        priority = calculatePriority()
    }

    def beforeUpdate() {
        priority = calculatePriority()
    }

    /**
     * Global bit/flag definition for the priority calculation.
     * <p>
     * The lowest bit is reserved for the property {@link #selectorType}, which needs special treatment.
     * All others are array types and will be checked if they contain any value.
     * In total there are 7 properties with 7 bits. The highest bit is unused and remains 0.
     * Integer is used to hold the bits and to be stored in database
     * <p>
     * @see: <a href="https://one-touch-pipeline.myjetbrains.com/youtrack/issue/otp-913">otp-913</a>
     */
    static final Map<String, Integer> PROPS_PRIORITY_MAP = [
            selectorType          : 0b0001000000000000,
            // reserved bit
            // reserved bit
            projects              : 0b0000001000000000,
            // reserved bit
            // reserved bit
            libraryPreparationKits: 0b0000000001000000,
            referenceGenomes      : 0b0000000000100000,
            seqTypes              : 0b0000000000010000,
            // reserved bit
            workflowVersions      : 0b0000000000000100,
            workflows             : 0b0000000000000010,
    ].asImmutable()

    /**
     * Calculate the priority by adding the given bit defined in the map {@link #PROPS_PRIORITY_MAP}
     * The algorithms distinguishes only if the property (e.g. workflows) has value (bit = 1) or not (bit = 0).
     * The values themself don't change the bit.
     */
    private int calculatePriority() {
        int prio = 0b0000000000000000
        PROPS_PRIORITY_MAP.each { String key, Integer value ->
            // checks 1. if the array property contains elements, 2. if the selectorType value is DEFAULT_VALUES
            if (key == "selectorType" ? this[key] != SelectorType.DEFAULT_VALUES : this[key] && !this[key].isEmpty()) {
                prio |= value
            }
        }
        return prio
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
