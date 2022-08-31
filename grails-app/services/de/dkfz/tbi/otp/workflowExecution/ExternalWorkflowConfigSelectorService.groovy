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
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@Slf4j
@Transactional(readOnly = true)
class ExternalWorkflowConfigSelectorService {

    ConfigSelectorService configSelectorService

    ExternalWorkflowConfigSelector getById(long id) {
        return ExternalWorkflowConfigSelector.get(id)
    }

    ExternalWorkflowConfigSelector findExactlyOneByExternalWorkflowConfigFragment(ExternalWorkflowConfigFragment fragment) {
        return CollectionUtils.exactlyOneElement(
                ExternalWorkflowConfigSelector.findAllByExternalWorkflowConfigFragment(fragment))
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

    List<NameValueAndConflictingKeysExternalWorkflowConfigSelector> getNameAndConfigValueOfMoreSpecificSelectors(
            List<Long> workflowIds, List<Long> workflowVersionIds, List<Long> projectIds, List<Long> referenceGenomeIds,
            List<Long> seqTypeIds, List<Long> libraryPreparationKitIds) {
        return ExternalWorkflowConfigSelector.createCriteria().list {
            and {
                if (workflowIds) {
                    or {
                        workflows {
                            'in'('id', workflowIds)
                        }
                        isEmpty('workflows')
                    }
                }
                if (workflowVersionIds) {
                    or {
                        workflowVersions {
                            'in'('id', workflowVersionIds)
                        }
                        isEmpty('workflowVersions')
                    }
                }
                if (projectIds) {
                    or {
                        projects {
                            'in'('id', projectIds)
                        }
                        isEmpty('projects')
                    }
                }
                if (seqTypeIds) {
                    or {
                        seqTypes {
                            'in'('id', seqTypeIds)
                        }
                        isEmpty('seqTypes')
                    }
                }
                if (referenceGenomeIds) {
                    or {
                        referenceGenomes {
                            'in'('id', referenceGenomeIds)
                        }
                        isEmpty('referenceGenomes')
                    }
                }
                if (libraryPreparationKitIds) {
                    or {
                        libraryPreparationKits {
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
                    selector.projects*.name)
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

class NameValueAndConflictingKeysExternalWorkflowConfigSelector {
    List<JsonConflictingParameters> conflictingParameters
    String selectorName
    String configFragmentValue
    List<String> projectNames

    NameValueAndConflictingKeysExternalWorkflowConfigSelector(String selectorName, String configFragmentValue, List<String> projectNames) {
        this.selectorName = selectorName
        this.configFragmentValue = configFragmentValue
        this.projectNames = projectNames
    }
}

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

