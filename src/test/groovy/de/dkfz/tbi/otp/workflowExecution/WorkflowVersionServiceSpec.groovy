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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils

class WorkflowVersionServiceSpec extends Specification implements ServiceUnitTest<WorkflowVersionService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [Workflow,
                WorkflowVersion,]
    }

    void "findAllByWorkflowId should return all workflowVersion for workflow by id"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowVersion workflowVersion1 = createWorkflowVersion([workflow: workflow])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([workflow: workflow])
        createWorkflowVersion()

        expect:
        CollectionUtils.containSame(service.findAllByWorkflowId(workflow.id), [workflowVersion1, workflowVersion2])
    }

    @Unroll
    void "setDeprecationState should set the deprecation date #text and comment if deprecate is #deprecate"() {
        given:
        service.commentService = Mock(CommentService)
        WorkflowVersion workflowVersion = createWorkflowVersion()
        String comment = "Test comment"

        when:
        WorkflowVersion result = service.updateWorkflowVersion(workflowVersion.id, comment, deprecate)

        then:
        result == workflowVersion
        (workflowVersion.deprecatedDate !== null) == deprecate

        and:
        1 * service.commentService.saveComment(workflowVersion, comment)

        where:
        text      | deprecate
        ''        | true
        'to null' | false
    }
}
