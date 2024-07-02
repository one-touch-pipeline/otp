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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.project.Project

class ProcessingPriorityServiceSpec extends Specification implements DataTest, DomainFactoryCore, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                WorkflowRun,
                Workflow,
                WorkflowVersion,
        ]
    }

    ProcessingPriorityService service

    static final String DEFAULT_PROCESSING_PRIORITY = "Normal"

    void setup() {
        service = new ProcessingPriorityService()
        service.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(_) >> DEFAULT_PROCESSING_PRIORITY
        }
    }

    void "test various methods for the processing priorities"() {
        given:
        ProcessingPriority processingPriority = createProcessingPriority([:], false)

        expect:
        service.priorityListCount == 0

        when:
        service.savePriority(processingPriority)

        then:
        service.priorityList == [processingPriority]
        service.priorityListCount == 1
        service.findByName(processingPriority.name) == processingPriority

        when:
        service.deletePriority(processingPriority.id)

        then:
        service.priorityListCount == 0
    }

    void "getReferences, should find all projects and workflow runs where priority is referenced"() {
        given:
        ProcessingPriority processingPriority = createProcessingPriority()

        createProject()
        createWorkflowRun()
        Project project1 = createProject([processingPriority: processingPriority])
        Project project2 = createProject([processingPriority: processingPriority])
        WorkflowRun wr1 = createWorkflowRun([priority: processingPriority])
        WorkflowRun wr2 = createWorkflowRun([priority: processingPriority])

        expect:
        TestCase.assertContainSame(service.getReferences(processingPriority.id), [
                (project1.id): Project.simpleName,
                (project2.id): Project.simpleName,
                (wr1.id)     : WorkflowRun.simpleName,
                (wr2.id)     : WorkflowRun.simpleName,
        ])
    }

    void "getReferences, should show when processing priority is default"() {
        given:
        ProcessingPriority processingPriority = createProcessingPriority([name: DEFAULT_PROCESSING_PRIORITY])

        createProject()
        createWorkflowRun()
        Project project = createProject([processingPriority: processingPriority])
        WorkflowRun wr = createWorkflowRun([priority: processingPriority])

        expect:
        TestCase.assertContainSame(service.getReferences(processingPriority.id), [
                (DEFAULT_PROCESSING_PRIORITY): ProcessingOption.simpleName,
                (project.id)                 : Project.simpleName,
                (wr.id)                      : WorkflowRun.simpleName,
        ])
    }
}
