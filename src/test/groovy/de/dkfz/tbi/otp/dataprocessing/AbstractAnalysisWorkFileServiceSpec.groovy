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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

class AbstractAnalysisWorkFileServiceSpec extends Specification implements DataTest {

    static final protected String EXPECTED_WORKFLOW_DIRECTORY_NAME = 'workflowDirectoryName'

    private final AbstractAnalysisWorkFileService service = new MockAbstractAnalysisWorkFileService()

    void "getWorkflowDirectoryName returns correct directory name"() {
        expect:
        service.workflowDirectoryName == 'workflowDirectoryName'
    }

    void "constructInstanceName returns correct instance name"() {
        given:
        TestConfigService testConfigService = new TestConfigService()
        service.configService = testConfigService

        and: 'fixed clock'
        testConfigService.fixClockTo(2000, 1, 1, 0, 0, 0)

        and: 'create workflow version'
        WorkflowVersion workflowVersion = new WorkflowVersion([
                workflowVersion: 'workflowVersion',
        ])

        and: 'define expected value'
        String expected = [
                "results",
                "_",
                EXPECTED_WORKFLOW_DIRECTORY_NAME,
                "-",
                workflowVersion.workflowVersion,
                "_",
                "2000-01-01-00-00-00",
        ].join('')

        expect:
        service.constructInstanceName(workflowVersion) == expected
    }

    static class MockAbstractAnalysisWorkFileService extends AbstractAnalysisWorkFileService {

        final String workflowDirectoryName = EXPECTED_WORKFLOW_DIRECTORY_NAME
    }
}
