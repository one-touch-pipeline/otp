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

class WorkflowBeanNameServiceSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
        ]
    }

    final List<String> classNames = ['Wfl1', 'Wfl2' ]
    final List<String> beanNames  = ['wfl1', 'wfl2' ]

    void setupData() {
        createWorkflow([
                name: classNames[0],
                beanName: beanNames[0],
                enabled: true,
        ])
    }

    void "method should return empty list, if the workflow class implemented OtpWorkflow has bean name in Workflow"() {
        given:
        setupData()
        WorkflowBeanNameService service = Spy(WorkflowBeanNameService) {
            _ * getImplementedWorkflowBeanNames()  >> [beanNames[0] ]
        }

        expect:
        service.findWorkflowBeanNamesNotSet().size() == 0
    }

    void "method should return a list with one entry, if we have two impl workflow classes"() {
        given:
        setupData()
        WorkflowBeanNameService service = Spy(WorkflowBeanNameService) {
            _ * getImplementedWorkflowBeanNames()  >> beanNames
        }

        expect:
        service.findWorkflowBeanNamesNotSet().size() == 1
        service.findWorkflowBeanNamesNotSet()[0]     == beanNames[1]
    }
}
