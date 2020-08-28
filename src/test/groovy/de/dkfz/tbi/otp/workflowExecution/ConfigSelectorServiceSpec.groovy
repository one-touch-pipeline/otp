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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

class ConfigSelectorServiceSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, ServiceUnitTest<ConfigSelectorService> {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ExternalWorkflowConfigSelector,
        ]
    }

    ExternalWorkflowConfigSelector createEWCSHelperBaseCriteria(String name, int basePriority) {
        return createExternalWorkflowConfigSelector([
                name              : name,
                basePriority      : basePriority,
        ])
    }

    @Unroll
    void "test findRelatedSelectorsByName #x"() {
        given:
        createEWCSHelperBaseCriteria("ewcs1", 0)
        createEWCSHelperBaseCriteria("ewcs2", 3)
        createEWCSHelperBaseCriteria("ewcs3", 1)
        createEWCSHelperBaseCriteria("ewcs4", 7)

        expect:
        CollectionUtils.containSame(result, service.findRelatedSelectorsByName(name())*.name)

        where:
        x | name        | result
        1 | { "ewcs1" } | ["ewcs1"]
        2 | { "wcs" }   | ["ewcs1", "ewcs2", "ewcs3", "ewcs4"]
        3 | { "" }      | []
        4 | { null }    | []
    }

    void "test SingleSelectSelectorExtendedCriteria, anyValueSet"() {
        given:
        Project project1 = createProject(name: "p1")

        SingleSelectSelectorExtendedCriteria singleSelectSelectorExtendedCriteria1 = new SingleSelectSelectorExtendedCriteria(project: project1)
        SingleSelectSelectorExtendedCriteria singleSelectSelectorExtendedCriteria2 = new SingleSelectSelectorExtendedCriteria()

        expect:
        singleSelectSelectorExtendedCriteria1.anyValueSet()
        !singleSelectSelectorExtendedCriteria2.anyValueSet()
    }

    void "test MultiSelectSelectorExtendedCriteria, anyValueSet"() {
        given:
        Project project1 = createProject(name: "p1")

        MultiSelectSelectorExtendedCriteria multiSelectSelectorExtendedCriteria1 = new MultiSelectSelectorExtendedCriteria(projects: [project1])
        MultiSelectSelectorExtendedCriteria multiSelectSelectorExtendedCriteria2 = new MultiSelectSelectorExtendedCriteria()

        expect:
        multiSelectSelectorExtendedCriteria1.anyValueSet()
        !multiSelectSelectorExtendedCriteria2.anyValueSet()
    }
}
