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

import grails.converters.JSON

class ConfigFragmentService {
    ConfigSelectorService configSelectorService

    List<ExternalWorkflowConfigFragment> getSortedFragments(SingleSelectSelectorExtendedCriteria singleSelectSelectorExtendedCriteria) {
        return configSelectorService.findAllSelectorsSortedByPriority(
                singleSelectSelectorExtendedCriteria
        )*.externalWorkflowConfigFragment
    }

    String mergeSortedFragments(List<ExternalWorkflowConfigFragment> fragments) {
        return (mergeSortedMaps(fragments*.configValuesToMap()) as JSON).toString()
    }

    private Map mergeSortedMaps(List<Map> prioritySortedHashMaps) {
        if (prioritySortedHashMaps) {
            Map combinedConfiguration = prioritySortedHashMaps.remove(0)
            return mergeSortedFragmentsRec(combinedConfiguration, prioritySortedHashMaps)
        }
        return [:]
    }

    @SuppressWarnings("Instanceof")
    private Map mergeSortedFragmentsRec(Map combinedConfigurationPart, List<Map> maps) {
        maps.each { Map map ->
            map.entrySet().each { Map.Entry entry ->
                if (entry.value instanceof Map) {  // case internal node
                    // if key not existing then create it with subtree and don't go into recursion
                    if (!(entry.key in combinedConfigurationPart.keySet())) {
                        combinedConfigurationPart.put(entry.key, entry.value)
                        return combinedConfigurationPart
                    }
                    // if key already existing go deeper to update eventually
                    mergeSortedFragmentsRec(combinedConfigurationPart[entry.key] as Map, [entry.value as Map])
                } else {  // case leaf
                    // only add when key not already set
                    if (!(entry.key in combinedConfigurationPart.keySet())) {
                        combinedConfigurationPart.put(entry.key, entry.value)
                    }
                }
                return combinedConfigurationPart
            }
        }
        return combinedConfigurationPart
    }
}
