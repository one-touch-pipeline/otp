/*
 * Copyright 2011-2022 The OTP authors
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

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Slf4j
@Transactional(readOnly = true)
class ExternalWorkflowConfigSelectorService {

    ConfigSelectorService configSelectorService

    /**
     * Calculate the priority by adding the given bit defined in the map {@link CalculatePriorityDTO#PROPS_PRIORITY_MAP}
     * The algorithms distinguishes only if the property (e.g. workflows) has value (bit = 1) or not (bit = 0).
     * The values themself don't change the bit.
     */
    static int calculatePriority(CalculatePriorityDTO dto) {
        int prio = 0b0000000000000000
        CalculatePriorityDTO.PROPS_PRIORITY_MAP.each { String key, Integer value ->
            // checks 1. if the array property contains elements, 2. if the selectorType value is DEFAULT_VALUES
            if (key == "selectorType" ? dto[key] != SelectorType.DEFAULT_VALUES : dto[key] && !dto[key].isEmpty()) {
                prio |= value
            }
        }
        return prio
    }

    ExternalWorkflowConfigSelector getById(long id) {
        return ExternalWorkflowConfigSelector.get(id)
    }

    List<JsonConflictingParameters> getAllConflictingConfigValues(String configValue1, String configValue2) {
        if (!configValue1 || !configValue2 || configValue1 == configValue2) {
            return []
        }
        Object json1 = new JsonSlurper().parseText(configValue1)
        Object json2 = new JsonSlurper().parseText(configValue2)
        return getConflictingKeysForJson(json1, json2)
    }

    /**
     * @param json1 : The current json object that should be compared
     * @param json2 : The json the first one should be compared to
     * @param completeKey : When recursively searching though the json, this parameter saves the already visited nodes.
     * @return a map with key of the json argument as first parameter and an object that contains the currentValue and the otherValue as value.
     */
    List<JsonConflictingParameters> getConflictingKeysForJson(Object json1, Object json2, String completeKey = '') {
        List<JsonConflictingParameters> conflictingKeys = []
        json1.each { String key, value ->
            String newCompleteKey = completeKey ? "${completeKey}.${key}" : key
            if ((!value && json2[key] && !(value instanceof Map)) || (value && !json2[key] && !(json2[key] instanceof Map))) {
                return
            }
            if (!(json1[key] instanceof Map && json2[key] instanceof Map)) {
                if (json1[key] != json2[key]) {
                    conflictingKeys.add(new JsonConflictingParameters(newCompleteKey, json1[key].toString(), json2[key].toString()))
                }
                return
            }
            conflictingKeys.addAll(getConflictingKeysForJson(json1[key], json2[key], newCompleteKey))
        }
        return conflictingKeys
    }

    /**
     * This method should return all overwriting, substituted and conflicting selectors that are provided in the
     * database in regards to the passed values for a selectors. A selector that has a more specific configuration
     * will overwrite/substitude values of the according selector. A selector that has less specific configurations
     * will be overwritten by the according selector.
     *
     * @param workflowIds : The ids of the workflows for the selector to check against.
     * @param workflowVersionIds : The ids of the workflow versions for the selector to check against.
     * @param projectIds : The ids of the projects for the selector to check against.
     * @param referenceGenomeIds : The ids of the reference genomes for the selector to check against.
     * @param seqTypeIds : The ids of the seqTypes for the selector to check against.
     * @param libraryPreparationKitIds : The ids of the library preparation kits for the selector to check against.
     * @param priority : The priority for the selector to check against.
     * @returns the grouped selectors that are overwriting, substituted and conflicting with the given selector
     */
    OverwritingSubstitutedAndConflictingSelectors getOverwritingSubstitutedAndConflictingSelectors(
            List<Long> workflowIds, List<Long> workflowVersionIds, List<Long> projectIds, List<Long> referenceGenomeIds,
            List<Long> seqTypeIds, List<Long> libraryPreparationKitIds, int priority) {

        List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> allConflictingSelectors = getAllPotentiallyProblematicSelectors(
                workflowIds, workflowVersionIds, projectIds, referenceGenomeIds, seqTypeIds, libraryPreparationKitIds,
        )

        Map<String, List<NameValueAndConflictingKeysExternalWorkflowConfigSelector>> groupedSelectors = allConflictingSelectors.groupBy {
            if (it.selectorPriority > priority) {
                return "overwritingSelectors"
            }
            if (it.selectorPriority < priority) {
                return "substitutedSelectors"
            }
            if (it.selectorPriority == priority) {
                return "conflictingSelectors"
            }
        }

        return new OverwritingSubstitutedAndConflictingSelectors(
                groupedSelectors.overwritingSelectors, groupedSelectors.substitutedSelectors, groupedSelectors.conflictingSelectors
        )
    }

    private List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> getAllPotentiallyProblematicSelectors(
            List<Long> workflowIds, List<Long> workflowVersionIds, List<Long> projectIds,
            List<Long> referenceGenomeIds, List<Long> seqTypeIds, List<Long> libraryPreparationKitIds) {
        return ExternalWorkflowConfigSelector.createCriteria().listDistinct {
            and {
                if (workflowIds) {
                    or {
                        workflows(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            'in'('id', workflowIds)
                        }
                        isEmpty('workflows')
                    }
                }
                if (workflowVersionIds) {
                    or {
                        workflowVersions(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            'in'('id', workflowVersionIds)
                        }
                        isEmpty('workflowVersions')
                    }
                }
                if (projectIds) {
                    or {
                        projects(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            'in'('id', projectIds)
                        }
                        isEmpty('projects')
                    }
                }
                if (seqTypeIds) {
                    or {
                        seqTypes(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            'in'('id', seqTypeIds)
                        }
                        isEmpty('seqTypes')
                    }
                }
                if (referenceGenomeIds) {
                    or {
                        referenceGenomes(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            'in'('id', referenceGenomeIds)
                        }
                        isEmpty('referenceGenomes')
                    }
                }
                if (libraryPreparationKitIds) {
                    or {
                        libraryPreparationKits(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                            'in'('id', libraryPreparationKitIds)
                        }
                        isEmpty('libraryPreparationKits')
                    }
                }
            }
        }.collect { ExternalWorkflowConfigSelector selector ->
            new NameValueAndConflictingKeysExternalWorkflowConfigSelector(
                    selector.name,
                    selector.externalWorkflowConfigFragment.configValues,
                    selector.projects*.name,
                    selector.priority,
            )
        }
    }

    ExternalWorkflowConfigSelectorLists searchExternalWorkflowConfigSelectors(ExternalWorkflowConfigSelectorSearchParameter searchParameter) {
        List<ExternalWorkflowConfigSelector> relatedSelectors, exactlyMatchingSelectors
        if (searchParameter.hasIds()) {
            Set<Workflow> selectedWorkflows = searchParameter.workflowIds.collect {
                CollectionUtils.exactlyOneElement(Workflow.findAllById(it))
            } as Set<Workflow>
            Set<WorkflowVersion> selectedWorkflowVersions = searchParameter.workflowVersionIds.collect {
                CollectionUtils.exactlyOneElement(WorkflowVersion.findAllById(it))
            } as Set<WorkflowVersion>
            Set<Project> selectedProjects = searchParameter.projectIds.collect {
                CollectionUtils.exactlyOneElement(Project.findAllById(it))
            } as Set<Project>
            Set<SeqType> selectedSeqTypes = searchParameter.seqTypeIds.collect {
                CollectionUtils.exactlyOneElement(SeqType.findAllById(it))
            } as Set<SeqType>
            Set<ReferenceGenome> selectedReferenceGenomes = searchParameter.referenceGenomeIds.collect {
                CollectionUtils.exactlyOneElement(ReferenceGenome.findAllById(it))
            } as Set<ReferenceGenome>
            Set<LibraryPreparationKit> selectedLibraryPreparationKits = searchParameter.libraryPreparationKitIds.collect {
                CollectionUtils.exactlyOneElement(LibraryPreparationKit.findAllById(it))
            } as Set<LibraryPreparationKit>

            MultiSelectSelectorExtendedCriteria criteria = new MultiSelectSelectorExtendedCriteria(
                    selectedWorkflows,
                    selectedWorkflowVersions,
                    selectedProjects,
                    selectedSeqTypes,
                    selectedReferenceGenomes,
                    selectedLibraryPreparationKits
            )

            exactlyMatchingSelectors = configSelectorService.findExactSelectors(criteria)

            relatedSelectors = configSelectorService.findAllRelatedSelectors(criteria)
            if (searchParameter.type) {
                relatedSelectors = relatedSelectors.findAll { ExternalWorkflowConfigSelector sel ->
                    searchParameter.type.contains(sel.selectorType.toString())
                }
            }
        } else {
            exactlyMatchingSelectors = []
            relatedSelectors = ExternalWorkflowConfigSelector.findAllBySelectorType(
                    SelectorType.DEFAULT_VALUES
            )
        }
        return new ExternalWorkflowConfigSelectorLists(relatedSelectors, exactlyMatchingSelectors)
    }
}

/**
 * A helper class to congregate all selectors that could be problematic for a checked selector.
 * These contain
 * - overwritingSelectors, that are overwriting the according selector.
 * - substitutedSelectors, which get overwritten by the according selector
 * - conflictingSelectors, which have same priority and will be potentially cause a problem *
 */
@CompileDynamic
class OverwritingSubstitutedAndConflictingSelectors {
    List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> overwritingSelectors
    List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> substitutedSelectors
    List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> conflictingSelectors

    List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> getAllSelectors() {
        return (overwritingSelectors + substitutedSelectors + conflictingSelectors)
    }

    OverwritingSubstitutedAndConflictingSelectors(
            List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> overwritingSelectors,
            List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> substitutedSelectors,
            List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> conflictingSelectors
    ) {
        this.overwritingSelectors = overwritingSelectors ?: []
        this.substitutedSelectors = substitutedSelectors ?: []
        this.conflictingSelectors = conflictingSelectors ?: []
    }
}

@CompileDynamic
class NameValueAndConflictingKeysExternalWorkflowConfigSelector {
    List<JsonConflictingParameters> conflictingParameters
    String selectorName
    int selectorPriority
    String configFragmentValue
    List<String> projectNames

    NameValueAndConflictingKeysExternalWorkflowConfigSelector(
            String selectorName, String configFragmentValue, List<String> projectNames, int selectorPriority
    ) {
        this.selectorName = selectorName
        this.configFragmentValue = configFragmentValue
        this.projectNames = projectNames
        this.selectorPriority = selectorPriority
    }
}

@CompileDynamic
class JsonConflictingParameters {
    String conflictingKey
    String currentValue
    String otherValue

    JsonConflictingParameters(String conflictingKey, String currentValue, String otherValue) {
        this.conflictingKey = conflictingKey
        this.currentValue = currentValue
        this.otherValue = otherValue
    }

    @Override
    String toString() {
        return "${conflictingKey} (current: ${currentValue}, other: ${otherValue})"
    }
}

@CompileDynamic
class CalculatePriorityDTO {
    /**
     * Global bit/flag definition for the priority calculation.
     * <p>
     * The lowest bit is reserved for the property selectorType, which needs special treatment.
     * All others are array types and will be checked if they contain any value.
     * In total there are 7 properties with 7 bits. The highest bit is unused and remains 0.
     * Integer is used to hold the bits and to be stored in database
     * <p>
     * @see: <a href="https://one-touch-pipeline.myjetbrains.com/youtrack/issue/otp-913">otp-913</a>
     */
    public static final Map<String, Integer> PROPS_PRIORITY_MAP = [
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

    SelectorType selectorType
    Set<Project> projects
    Set<LibraryPreparationKit> libraryPreparationKits
    Set<ReferenceGenome> referenceGenomes
    Set<SeqType> seqTypes
    Set<WorkflowVersion> workflowVersions
    Set<Workflow> workflows
}
