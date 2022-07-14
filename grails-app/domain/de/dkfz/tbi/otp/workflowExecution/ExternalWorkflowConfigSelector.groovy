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
            workflows             : 0b01000000,
            workflowVersions      : 0b00100000,
            seqTypes              : 0b00010000,
            referenceGenomes      : 0b00001000,
            libraryPreparationKits: 0b00000100,
            projects              : 0b00000010,
            selectorType          : 0b00000001,
    ].asImmutable()

    /**
     * Calculate the priority by checking the given bit defined in the map {@link #PROPS_PRIORITY_MAP}
     * is occupied. The algorithms distinguishes if the property (e.g. workflows) has value (bit = 1) or not (bit = 0).
     * The values themself don't change the bit.
     */
    private int calculatePriority() {
        int prio = 0b00000000
        PROPS_PRIORITY_MAP.each { String key, Integer value ->
            // checks 1. if the array contains elements, 2. if the value is default values
            if (value > 1 ? this[key] && !this[key].isEmpty() : this[key] != SelectorType.DEFAULT_VALUES) {
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
