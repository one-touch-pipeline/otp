/*
 * Copyright 2011-2023 The OTP authors
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

import grails.testing.services.ServiceUnitTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class OtpWorkflowServiceSpec extends Specification implements ServiceUnitTest<OtpWorkflowService>, DomainFactoryCore {

    void "lookupAlignableOtpWorkflowBeans, when called, should return all alignable OtpWorkflows"() {
        given:
        Map<String, OtpWorkflow> otpWorkflowAlignable = (1..3).collectEntries {
            [("align_${it}" as String): Mock(OtpWorkflow) {
                1 * isAlignment() >> true
            }]
        }
        Map<String, OtpWorkflow> otpWorkflowNotAlignable = (1..3).collectEntries {
            [("notAlign_${it}" as String): Mock(OtpWorkflow) {
                1 * isAlignment() >> false
            }]
        }

        and:
        Map<String, OtpWorkflow> otpWorkflowMap = otpWorkflowAlignable + otpWorkflowNotAlignable

        service.applicationContext = Mock(ApplicationContext) {
            1 * getBeansOfType(OtpWorkflow) >> otpWorkflowMap
            0 * _
        }

        when:
        Map<String, OtpWorkflow> foundOtpWorkflows = service.lookupAlignableOtpWorkflowBeans()

        then:
        foundOtpWorkflows == otpWorkflowAlignable
    }
}
