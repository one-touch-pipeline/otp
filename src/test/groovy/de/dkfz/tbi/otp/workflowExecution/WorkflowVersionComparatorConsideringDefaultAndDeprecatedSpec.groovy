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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

import java.time.LocalDate
import java.time.Month

class WorkflowVersionComparatorConsideringDefaultAndDeprecatedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowVersion,
                Workflow,
        ]
    }

    void "compare should sort Workflow Versions by default version, normal version and deprecated versions last"() {
        given:
        WorkflowApiVersion apiVersion = createWorkflowApiVersion()
        WorkflowVersion defaultVersion = createWorkflowVersion([apiVersion: apiVersion,])
        WorkflowVersionComparatorConsideringDefaultAndDeprecated comparator = new WorkflowVersionComparatorConsideringDefaultAndDeprecated(defaultVersion)
        WorkflowVersion deprecatedVersion1 = createWorkflowVersion([apiVersion: apiVersion, deprecatedDate: LocalDate.of(2020, Month.APRIL, 8), workflowVersion: "1.0.1"])
        WorkflowVersion deprecatedVersion2 = createWorkflowVersion([apiVersion: apiVersion, deprecatedDate: LocalDate.of(2020, Month.MARCH, 7), workflowVersion: "2.0.13"])
        WorkflowVersion deprecatedVersion3 = createWorkflowVersion([apiVersion: apiVersion, deprecatedDate: LocalDate.of(2022, Month.JANUARY, 20), workflowVersion: "1.3.2.4"])
        WorkflowVersion deprecatedVersion4 = createWorkflowVersion([apiVersion: apiVersion, deprecatedDate: LocalDate.of(2022, Month.JANUARY, 20), workflowVersion: "1.3"])
        WorkflowVersion deprecatedVersion5 = createWorkflowVersion([apiVersion: apiVersion, deprecatedDate: LocalDate.of(2022, Month.JANUARY, 20), workflowVersion: "1.3.2"])
        WorkflowVersion workflowVersion1 = createWorkflowVersion([apiVersion: apiVersion, workflowVersion: "1.0.0"])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([apiVersion: apiVersion, workflowVersion: "3.4.2"])
        WorkflowVersion workflowVersion3 = createWorkflowVersion([apiVersion: apiVersion, workflowVersion: "1.2.3"])
        WorkflowVersion workflowVersion4 = createWorkflowVersion([apiVersion: apiVersion, workflowVersion: "1.2.3.4"])
        WorkflowVersion workflowVersion5 = createWorkflowVersion([apiVersion: apiVersion, workflowVersion: "1.2"])
        List<WorkflowVersion> listToSort = [
                defaultVersion,
                deprecatedVersion1,
                deprecatedVersion2,
                deprecatedVersion3,
                deprecatedVersion4,
                deprecatedVersion5,
                workflowVersion1,
                workflowVersion2,
                workflowVersion3,
                workflowVersion4,
                workflowVersion5,
        ]

        when:
        listToSort.sort { a, b -> comparator.compare(a, b) }

        then:
        listToSort == [
                defaultVersion,
                workflowVersion2,
                workflowVersion4,
                workflowVersion3,
                workflowVersion5,
                workflowVersion1,
                deprecatedVersion2,
                deprecatedVersion3,
                deprecatedVersion5,
                deprecatedVersion4,
                deprecatedVersion1,
        ]
    }

    void "compare should sort Workflow Versions also if no default is configured"() {
        given:
        WorkflowApiVersion apiVersion = createWorkflowApiVersion()
        WorkflowVersionComparatorConsideringDefaultAndDeprecated comparator = new WorkflowVersionComparatorConsideringDefaultAndDeprecated()
        WorkflowVersion deprecatedVersion1 = createWorkflowVersion([apiVersion: apiVersion, deprecatedDate: LocalDate.of(2020, Month.APRIL, 8), workflowVersion: "1.0.1"])
        WorkflowVersion deprecatedVersion2 = createWorkflowVersion([apiVersion: apiVersion, deprecatedDate: LocalDate.of(2020, Month.MARCH, 7), workflowVersion: "2.0.13"])
        WorkflowVersion workflowVersion1 = createWorkflowVersion([apiVersion: apiVersion, workflowVersion: "1.0.0"])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([apiVersion: apiVersion, workflowVersion: "3.4.2"])
        List<WorkflowVersion> listToSort = [
                deprecatedVersion1,
                deprecatedVersion2,
                workflowVersion1,
                workflowVersion2,
        ]

        when:
        listToSort.sort { a, b -> comparator.compare(a, b) }

        then:
        listToSort == [
                workflowVersion2,
                workflowVersion1,
                deprecatedVersion2,
                deprecatedVersion1,
        ]
    }
}
