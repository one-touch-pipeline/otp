/*
 * Copyright 2011-2021 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationPrepareJob
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow

@Rollback
@Integration
class OtpWorkflowServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    OtpWorkflowService otpWorkflowService

    void "lookupOtpWorkflowBean, when name is an OtpWorkflow, then return the bean"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        beanName: DataInstallationWorkflow.simpleName.uncapitalize(),
                ]),
        ])

        when:
        OtpWorkflow otpWorkflow = otpWorkflowService.lookupOtpWorkflowBean(workflowRun)

        then:
        otpWorkflow.class == DataInstallationWorkflow
    }

    void "lookupOtpWorkflowBean, when name is a bean, but not an OtpWorkflow, then throw Exception"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        beanName: DataInstallationPrepareJob.simpleName.uncapitalize(),
                ]),
        ])

        when:
        otpWorkflowService.lookupOtpWorkflowBean(workflowRun)

        then:
        thrown(BeanNotOfRequiredTypeException)
    }

    void "lookupOtpWorkflowBean, when name is not a bean at all, then throw WorkflowNotFoundException"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        beanName: "NoBean_${nextId}",
                ]),
        ])

        when:
        otpWorkflowService.lookupOtpWorkflowBean(workflowRun)

        then:
        thrown(NoSuchBeanDefinitionException)
    }
}
