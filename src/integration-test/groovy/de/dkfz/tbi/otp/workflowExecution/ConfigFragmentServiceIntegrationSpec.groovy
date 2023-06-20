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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.utils.MapUtilService

@Rollback
@Integration
class ConfigFragmentServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    @Autowired
    MapUtilService mapUtilService

    private ConfigFragmentService service

    private List<ExternalWorkflowConfigSelector> selectors

    private List<ExternalWorkflowConfigFragment> prioritySortedHashMaps

    private JSONObject mergedJson

    private void setupData() {
        service = new ConfigFragmentService()
        service.mapUtilService = mapUtilService
    }

    private void setupDataWithSelectorsForQuerying() {
        setupData()

        ExternalWorkflowConfigSelector ewcs1 = createExternalWorkflowConfigSelector()
        ExternalWorkflowConfigSelector ewcs2 = createExternalWorkflowConfigSelector()
        ExternalWorkflowConfigSelector ewcs3 = createExternalWorkflowConfigSelector()
        selectors = [ewcs2, ewcs3, ewcs1]

        service.configSelectorService = Mock(ConfigSelectorService) {
            1 * findAllSelectorsSortedByPriority(_) >> selectors
        }
    }

    private void setupDataWithFragmentsForMerging() {
        setupData()

        prioritySortedHashMaps = [
                createExternalWorkflowConfigFragment(
                        configValues: '{"WORKFLOWS": {"A": "A_ShouldAppear", "B": {"C": "C_ThisSinceHigherPrio", "D": "D_ThisToo"}}}'
                ),
                createExternalWorkflowConfigFragment(
                        configValues: '{"WORKFLOWS": {"F": "F_ShouldAppear", "B": {"C": "C_ThisNotSinceLowerPrio", "E": {"G": "G_ThisToo"}}}}'
                ),
                createExternalWorkflowConfigFragment(
                        configValues: '{"WORKFLOWS": {"A": "A_ThisNotSinceLowerPrio", "B": {"C": "C_ThisNotSinceLowerPrio", "D": "D_ThisNotSinceLowerPrio"}}}'
                ),
        ]

        mergedJson = [
                "WORKFLOWS": [
                        "A": "A_ShouldAppear",
                        "B": [
                                "C": "C_ThisSinceHigherPrio",
                                "D": "D_ThisToo",
                                "E": [
                                        "G": "G_ThisToo",
                                ] as JSONObject,
                        ] as JSONObject,
                        "F": "F_ShouldAppear",
                ] as JSONObject,
        ] as JSONObject
    }

    void "test getSortedFragmentSelectors, when called, then return ExternalWorkflowConfigSelector in expected order"() {
        given:
        setupDataWithSelectorsForQuerying()

        expect:
        selectors == service.getSortedFragmentSelectors(new SingleSelectSelectorExtendedCriteria())
    }

    void "test getSortedFragments, when called, then return Fragments in expected order"() {
        given:
        setupDataWithSelectorsForQuerying()

        expect:
        selectors*.externalWorkflowConfigFragment == service.getSortedFragments(new SingleSelectSelectorExtendedCriteria())
    }

    void "test mergeSortedMaps"() {
        given:
        setupData()
        List<Map> prioritySortedHashMaps = [
                ["A": "A_ShouldAppear", "B": ["C": "C_ThisSinceHigherPrio", "D": "D_ThisToo"]],
                ["F": "F_ShouldAppear", "B": ["C": "C_ThisNotSinceLowerPrio", "E": ["G": "G_ThisToo"]]],
                ["A": "A_ThisNotSinceLowerPrio", "B": ["C": "C_ThisNotSinceLowerPrio", "D": "D_ThisNotSinceLowerPrio"]],
        ]

        expect:
        ["A": "A_ShouldAppear", "B": ["D": "D_ThisToo", "E": ["G": "G_ThisToo"], "C": "C_ThisSinceHigherPrio"], "F": "F_ShouldAppear"] ==
                service.mapUtilService.mergeSortedMaps(prioritySortedHashMaps)
    }

    void "test mergeSortedFragmentsAsJson, when called, then returned the merged json as jsonobject"() {
        given:
        setupDataWithFragmentsForMerging()

        expect:
        mergedJson == service.mergeSortedFragmentsAsJson(prioritySortedHashMaps)
    }

    void "test mergeSortedFragments, when called, then returned the merged json as string"() {
        given:
        setupDataWithFragmentsForMerging()

        expect:
        mergedJson.toString() == service.mergeSortedFragments(prioritySortedHashMaps)
    }
}
