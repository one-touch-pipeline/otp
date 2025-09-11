/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.shared.WorkflowTestException
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector
import de.dkfz.tbi.otp.workflowTest.WorkflowTestProperty

import java.nio.file.Paths

class TestConfigServiceSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalWorkflowConfigFragment,
                ExternalWorkflowConfigSelector,
        ]
    }

    TestConfigService configService

    void setup() {
        configService = new TestConfigService()
    }

    void cleanup() {
        configService.clean()
    }

    void "test storeWorkflowTestProperties() stores and validates properties without exception"() {
        given:
        Map<WorkflowTestProperty, String> workflowTestProperties = [
                (WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR): '/script/input',
                (WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR): '/script/result',
                (WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY): '/script/shared-files',
                (WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY): '/script/virtualenvs',
                (WorkflowTestProperty.TEST_WORKFLOW_QUEUE): 'script-queue',
                (WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX): 'script-suffix',
        ]

        when:
        configService.storeWorkflowTestProperties(workflowTestProperties)

        and:
        configService.validateWorkflowTestProperties()

        then:
        // Verify properties are stored
        configService.workflowTestProperties == workflowTestProperties
        // Verify validation passes (no exception thrown)
        noExceptionThrown()
        // Verify getters work correctly
        configService.workflowTestInputRootDir == Paths.get('/script/input')
        configService.workflowTestResultRootDir == Paths.get('/script/result')
        configService.workflowTestRoddySharedFilesBaseDir == Paths.get('/script/shared-files')
        configService.workflowTestRoddyVirtualEnvsBaseDir == Paths.get('/script/virtualenvs')
        configService.workflowTestQueue == 'script-queue'
        configService.workflowTestConfigSuffix == 'script-suffix'
    }

    void "test validateWorkflowTestProperties() throws exception for incomplete properties"() {
        given:
        Map<WorkflowTestProperty, String> workflowTestProperties = [
                (WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR): '/script/input',
                (WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR): '/script/result',
                // Missing required properties
        ]
        // Properties that are missing and should be reported
        List<WorkflowTestProperty> missingProperties = [
                WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY,
                WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY,
                WorkflowTestProperty.TEST_WORKFLOW_QUEUE,
                WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX,
        ]

        when:
        configService.storeWorkflowTestProperties(workflowTestProperties)

        and:
        configService.validateWorkflowTestProperties()

        then:
        WorkflowTestException e = thrown()
        e.message.contains('Required properties are missing:')
        missingProperties.each {
            assert e.message.contains(it.key)
        }
        (workflowTestProperties.keySet() - missingProperties).each {
            assert !e.message.contains(it.key)
        }
    }

    void "test validateWorkflowTestProperties() throws exception for invalid properties"() {
        given:
        Map<WorkflowTestProperty, String> workflowTestProperties = [
                (WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR): '/correct/absolute/path',
                (WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR): './wrong/relative/path',
                (WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY): 'false',
                (WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY): '[multiple paths]',
                (WorkflowTestProperty.TEST_WORKFLOW_QUEUE): 'not a single word',
                (WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX): 'multiple\nline\ntext',
        ]

        // Properties that are invalid and should be reported
        List<WorkflowTestProperty> invalidProperties = [
                WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR,
                WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY,
                WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY,
                WorkflowTestProperty.TEST_WORKFLOW_QUEUE,
                WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX,
        ]

        when:
        configService.storeWorkflowTestProperties(workflowTestProperties)

        and:
        configService.validateWorkflowTestProperties()

        then:
        WorkflowTestException e = thrown()
        e.message.contains('Invalid properties found:')
        invalidProperties.each {
            assert e.message.contains(it.key)
            assert e.message.contains(workflowTestProperties[it])
        }
        (workflowTestProperties.keySet() - invalidProperties).each {
            assert !e.message.contains(it.key)
        }
    }

    void "test clean() method resets script-based properties"() {
        given:
        configService.workflowTestProperties[WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR] = '/script/path'

        when:
        configService.clean()

        then:
        configService.workflowTestProperties.isEmpty()
    }
}
