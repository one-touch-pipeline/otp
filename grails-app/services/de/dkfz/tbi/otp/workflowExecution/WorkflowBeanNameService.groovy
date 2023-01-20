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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@CompileDynamic
class WorkflowBeanNameService {
    @Autowired
    ApplicationContext applicationContext

    /**
     * It fetches all workflow classes/beans implementing the {@link OtpWorkflow} trait and
     * check if its name is set in the {@link Workflow} domain object
     *
     * @return the list of classes, which don't have bean name in the {@link Workflow} domain object.
     */
    @Transactional(readOnly = true)
    List<String> findWorkflowBeanNamesNotSet() {
        List<String> beanNamesInDatabase = Workflow.withCriteria {
            projections {
                property('beanName')
            }
            isNotNull('beanName')
        }

        //check if the bean name in db is the same as the bean name of implemented workflows
        return implementedWorkflowBeanNames - beanNamesInDatabase
    }

    List<String> getImplementedWorkflowBeanNames() {
        return applicationContext.getBeanNamesForType(OtpWorkflow)
    }
}
