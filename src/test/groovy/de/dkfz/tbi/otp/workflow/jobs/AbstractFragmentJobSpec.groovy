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
package de.dkfz.tbi.otp.workflow.jobs

import grails.test.hibernate.HibernateSpec
import groovy.transform.TupleConstructor
import org.grails.testing.GrailsUnitTest
import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.workflow.shared.MultipleCombinedFragmentException
import de.dkfz.tbi.otp.workflowExecution.*

class AbstractFragmentJobSpec extends HibernateSpec implements GrailsUnitTest, WorkflowSystemDomainFactory {

    private AbstractFragmentJob job
    private WorkflowStep workflowStep
    private JSONObject combinedJsonObject

    @Override
    List<Class> getDomainClasses() {
        return [
                ExternalWorkflowConfigSelector,
                RawSequenceFile,
                WorkflowStep,
        ]
    }

    @Override
    Closure doWithSpring() {
        return { ->
            configSelectorService(ConfigSelectorService)
        }
    }

    void "execute, when only one criteria is used, then call expected methods and succeeds"() {
        given:
        setupDataInit()
        DataTuple dataTuple = createCriteria('single')

        when:
        job.execute(workflowStep)

        then:
        job.fetchSelectors(workflowStep) >> [dataTuple.criteria]
        1 * job.workflowRunService.saveCombinedConfig(workflowStep.workflowRun.id, dataTuple.jsonObjectString)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        1 * job.logService.addSimpleLogEntry(workflowStep, "Found ${1} selectors")
    }

    void "execute, when multiple criteria are used, returning the same set of selectors, then call expected methods and succeeds"() {
        given:
        setupDataInit()
        DataTuple dataTuple1 = createCriteria('sameSelectors1')
        DataTuple dataTuple2 = createCriteriaWithSelectors(dataTuple1.selectors)
        DataTuple dataTuple3 = createCriteriaWithSelectors(dataTuple1.selectors)

        when:
        job.execute(workflowStep)

        then:
        job.fetchSelectors(workflowStep) >> [dataTuple1.criteria, dataTuple2.criteria, dataTuple3.criteria]
        1 * job.workflowRunService.saveCombinedConfig(workflowStep.workflowRun.id, dataTuple1.jsonObjectString)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        1 * job.logService.addSimpleLogEntry(workflowStep, "Found ${3} selectors")
    }

    void "execute, when multiple criteria are used returning different set of selectors using fragments with same value, then call expected methods and succeeds"() {
        given:
        setupDataInit()
        DataTuple dataTuple1 = createCriteria('different final1')
        DataTuple dataTuple2 = createCriteriaWithGivenFragmentValues('different final2', dataTuple1.fragments)
        DataTuple dataTuple3 = createCriteriaWithGivenFragmentValues('different final3', dataTuple1.fragments)

        when:
        job.execute(workflowStep)

        then:
        job.fetchSelectors(workflowStep) >> [dataTuple1.criteria, dataTuple2.criteria, dataTuple3.criteria]
        1 * job.workflowRunService.saveCombinedConfig(workflowStep.workflowRun.id, dataTuple1.jsonObjectString)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        1 * job.logService.addSimpleLogEntry(workflowStep, "Found ${3} selectors")
    }

    void "execute, when multiple criteria are used returning different set of selectors whose fragments producing the same combinedFragment, then call expected methods and succeeds"() {
        given:
        setupDataInit()
        DataTuple dataTuple1 = createCriteria('different final1')
        DataTuple dataTuple2 = createCriteria('different final2')
        DataTuple dataTuple3 = createCriteria('different final3')

        when:
        job.execute(workflowStep)

        then:
        job.fetchSelectors(workflowStep) >> [dataTuple1.criteria, dataTuple2.criteria, dataTuple3.criteria]
        1 * job.workflowRunService.saveCombinedConfig(workflowStep.workflowRun.id, dataTuple1.jsonObjectString)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        1 * job.logService.addSimpleLogEntry(workflowStep, "Found ${3} selectors")
    }

    void "execute, when multiple criteria of different set of selectors having fragments with the same value"() {
        given:
        setupDataInit()
        DataTuple dataTuple1 = createCriteria('sameFragmentValues1')
        DataTuple dataTuple2 = createCriteria('sameFragmentValues2', [
                key: "combinedValue2",
        ] as JSONObject)
        DataTuple dataTuple3 = createCriteria('sameFragmentValues3', [
                key: "combinedValue3",
        ] as JSONObject)

        when:
        job.execute(workflowStep)

        then:
        job.fetchSelectors(workflowStep) >> [dataTuple1.criteria, dataTuple2.criteria, dataTuple3.criteria]
        1 * job.logService.addSimpleLogEntry(workflowStep, "Found ${3} selectors")
        1 * job.logService.addSimpleLogEntry(workflowStep, "Found ${3} different combined fragments")

        thrown(MultipleCombinedFragmentException)
    }

    private void setupDataInit() {
        applicationContext // need call for initialisation

        workflowStep = createWorkflowStep()
        combinedJsonObject = [
                key: "combinedValue",
        ] as JSONObject

        job = Spy(AbstractFragmentJob)

        job.configFragmentService = Mock(ConfigFragmentService) {
            0 * _
        }

        job.workflowRunService = Mock(WorkflowRunService) {
            0 * _
        }
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            0 * _
        }

        job.logService = Mock(LogService) {
            0 * _
        }
    }

    private DataTuple createCriteria(String caseName, JSONObject jsonObject = combinedJsonObject) {
        List<String> fragmentValues = (1..3).collect {
            "fragmentvalue for criteria_${caseName}_fragment_${it}"
        }
        return createCriteriaWithGivenFragmentValues(caseName, fragmentValues, jsonObject)
    }

    private DataTuple createCriteriaWithGivenFragmentValues(String caseName, List<String> fragmentValues, JSONObject jsonObject = combinedJsonObject) {
        List<ExternalWorkflowConfigSelector> selectors = fragmentValues.withIndex().collect {
            createExternalWorkflowConfigSelector([
                    name                          : "criteria_${caseName}_selector_${it.v2}",
                    externalWorkflowConfigFragment: createExternalWorkflowConfigFragment([
                            name        : "criteria_${caseName}_fragment_${it.v2}",
                            configValues: it.v1,
                    ]),

            ])
        }
        return createCriteriaWithSelectors(selectors, jsonObject)
    }

    private DataTuple createCriteriaWithSelectors(List<ExternalWorkflowConfigSelector> selectors, JSONObject jsonObject = combinedJsonObject) {
        SingleSelectSelectorExtendedCriteria criteria = new SingleSelectSelectorExtendedCriteria()
        List<ExternalWorkflowConfigFragment> fragments = selectors*.externalWorkflowConfigFragment
        String jsonObjectString = jsonObject

        1 * job.configFragmentService.getSortedFragmentSelectors(criteria) >> selectors
        1 * job.configFragmentService.mergeSortedFragmentsAsJson(fragments) >> jsonObject

        1 * job.logService.addSimpleLogEntry(workflowStep, "Handle ${criteria}")
        1 * job.logService.addSimpleLogEntry(workflowStep, "Found ${selectors.size()} fragments")
        1 * job.logService.addSimpleLogEntry(workflowStep) {
            it =~ /CombinedFragments:\n.*/
        }

        selectors.each { selector ->
            1 * job.logService.addSimpleLogEntry(workflowStep) {
                it =~ /name: ${selector.name}.*\npriority: ${selector.priority}\nfragment:\n.*/
            }
        }

        return new DataTuple(criteria, fragments, selectors, jsonObjectString)
    }

    @TupleConstructor
    static class DataTuple {
        SingleSelectSelectorExtendedCriteria criteria
        List<ExternalWorkflowConfigFragment> fragments
        List<ExternalWorkflowConfigSelector> selectors
        String jsonObjectString
    }
}
