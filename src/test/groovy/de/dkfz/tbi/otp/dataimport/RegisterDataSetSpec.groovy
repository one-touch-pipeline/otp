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
package de.dkfz.tbi.otp.dataimport

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.dataimport.DataImportDomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class RegisterDataSetSpec extends Specification implements DataTest, DataImportDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataType,
                DataTypeVersion,
                Key,
                ProcessingPriority,
                Project,
                RegisterDataSet,
                Ticket,
        ]
    }

    void "validate, when all values are given, then it is valid"() {
        expect:
        createRegisterDataSet([:], false).validate()
    }

    void "create, when state is not given, then it is WAITING"() {
        expect:
        createRegisterDataSet().state == WorkflowCreateState.WAITING
    }

    @Unroll
    void "validate, when #property is null, then it is invalid"() {
        expect:
        TestCase.assertAtLeastExpectedValidateError(createRegisterDataSet([(property): null], false), property, 'nullable', null)

        where:
        property << ['ticket', 'dataTypeVersion', 'priority', 'projects', 'rawJsonData', 'state']
    }

    void "validate, when projects is empty, then it is invalid"() {
        expect:
        TestCase.assertValidateError(createRegisterDataSet([projects: [] as Set<Project>], false), 'projects', 'minSize.notmet', [] as Set<Project>)
    }

    void "validate, when there are several projects, then it is valid"() {
        expect:
        createRegisterDataSet([projects: [createProject(), createProject()] as Set<Project>], false).validate()
    }

    @Unroll
    void "validate, when rawJsonData is '#data', then it is invalid"() {
        expect:
        TestCase.assertValidateError(createRegisterDataSet([rawJsonData: data], false), 'rawJsonData', DataImportSchemaValidator.INVALID_JSON, data)

        where:
        data << ['{"rows": [', 'no json', '{"a": 1,}']
    }

    @Unroll
    void "validate, when rawJsonData is the valid JSON '#data' despite not matching any schema, then it is valid"() {
        expect:
        createRegisterDataSet([rawJsonData: data], false).validate()

        where:
        data << ['{}', '[1, 2]', '"text"', '5', '{"unknown": {"nested": true}}']
    }
}
