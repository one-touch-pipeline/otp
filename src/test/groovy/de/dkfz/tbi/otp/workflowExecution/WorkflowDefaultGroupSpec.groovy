/*
 * Copyright 2011-2026 The OTP authors
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
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

class WorkflowDefaultGroupSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowDefaultGroup,
                SeqType,
        ]
    }

    void "validator, should prevent creating two WorkflowDefaultGroups with the same name"() {
        given:
        WorkflowDefaultGroup group = createWorkflowDefaultGroup(name: "sameName")

        when:
        createWorkflowDefaultGroup(name: group.name)

        then:
        ValidationException e = thrown(ValidationException)
        e.message.contains("unique")
    }

    void "validator, should allow creating WorkflowDefaultGroups with different names"() {
        given:
        createWorkflowDefaultGroup(name: "firstName")

        when:
        createWorkflowDefaultGroup(name: "secondName")

        then:
        notThrown(ValidationException)
    }

    void "validator, should prevent creating a WorkflowDefaultGroup with a blank name"() {
        when:
        createWorkflowDefaultGroup(name: "")

        then:
        ValidationException e = thrown(ValidationException)
        e.message.contains("blank")
    }

    void "validator, should prevent creating a WorkflowDefaultGroup without a seqType"() {
        when:
        createWorkflowDefaultGroup(seqType: null)

        then:
        ValidationException e = thrown(ValidationException)
        e.message.contains("nullable")
    }
}
