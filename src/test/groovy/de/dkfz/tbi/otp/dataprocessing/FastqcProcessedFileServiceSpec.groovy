/*
 * Copyright 2011-2022 The OTP authors
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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.FastqcWorkflowDomainFactory
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

class FastqcProcessedFileServiceSpec extends Specification implements ServiceUnitTest<FastQcProcessedFileService>, DataTest, FastqcDomainFactory, FastqcWorkflowDomainFactory {

    TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqcProcessedFile,
                ProcessingOption,
        ]
    }

    void setup() throws Exception {
        configService = new TestConfigService()
        configService.fixClockTo(2000, 1, 23, 15, 45, 55)
        configService.processingOptionService = new ProcessingOptionService()

        service.configService = configService
    }

    void cleanup() throws Exception {
        configService?.clean()
    }

    void "buildWorkingPath, when called for bash fastqc workflow, then return expected value"() {
        given:
        WorkflowVersion workflowVersion = createBashFastqcWorkflowVersion()
        String expected = [
                "bash",
                workflowVersion.workflowVersion,
                "2000-01-23-15-45-55-000",
        ].join('-')

        when:
        String result = service.buildWorkingPath(workflowVersion)

        then:
        result == expected
    }

    void "buildWorkingPath, when called for other workflow, then throw assertion"() {
        given:
        WorkflowVersion workflowVersion = createWorkflowVersion()

        when:
        service.buildWorkingPath(workflowVersion)

        then:
        thrown(AssertionError)
    }
}
