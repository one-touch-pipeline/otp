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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.time.LocalDate

class WorkflowVersionServiceSpec extends Specification implements ServiceUnitTest<WorkflowVersionService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [Workflow,
                WorkflowVersion,]
    }

    void "findAllByWorkflowId should return all workflowVersion for workflow by id"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowVersion workflowVersion1 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)])
        createWorkflowVersion()

        expect:
        CollectionUtils.containSame(service.findAllByWorkflowId(workflow.id), [workflowVersion1, workflowVersion2])
    }

    @Unroll
    void "updateWorkflowVersion should set the deprecation date #text and comment if deprecate is #deprecate and seq Type and Ref Genome"() {
        given:
        service.commentService = Mock(CommentService)
        service.mergingCriteriaService = Mock(MergingCriteriaService)

        WorkflowVersion workflowVersion = createWorkflowVersion()
        String comment = "Test comment"
        SeqType seqType1 = createSeqType()
        SeqType seqType2 = createSeqType()
        ReferenceGenome refGenome = createReferenceGenome()

        UpdateWorkflowVersionDto updateDto = new UpdateWorkflowVersionDto([
                workflowVersionId: workflowVersion.id,
                comment          : comment,
                deprecate        : deprecate,
                allowedRefGenomes: [refGenome.id],
                supportedSeqTypes: [seqType1.id, seqType2.id],
        ])

        when:
        WorkflowVersion result = service.updateWorkflowVersion(updateDto)

        then:
        result == workflowVersion
        TestCase.assertContainSame(result.supportedSeqTypes, [seqType1, seqType2] as Set)
        result.allowedReferenceGenomes == [refGenome] as Set
        (workflowVersion.deprecatedDate !== null) == deprecate

        and:
        1 * service.commentService.saveComment(workflowVersion, comment)
        1 * service.mergingCriteriaService.createDefaultMergingCriteria(seqType1)
        1 * service.mergingCriteriaService.createDefaultMergingCriteria(seqType2)

        where:
        text      | deprecate
        ''        | true
        'to null' | false
    }

    void "updateWorkflowVersion, should not set a new deprecation date if it was already deprecated"() {
        given:
        service.commentService = Mock(CommentService)
        service.mergingCriteriaService = Mock(MergingCriteriaService)

        LocalDate deprecatedDate = LocalDate.of(2000, 10, 20)
        WorkflowVersion workflowVersion = createWorkflowVersion([
                deprecatedDate: deprecatedDate,
        ])
        String comment = "Test comment"
        SeqType seqType = createSeqType()
        ReferenceGenome refGenome = createReferenceGenome()

        UpdateWorkflowVersionDto updateDto = new UpdateWorkflowVersionDto([
                workflowVersionId: workflowVersion.id,
                comment          : comment,
                deprecate        : true,
                allowedRefGenomes: [refGenome.id],
                supportedSeqTypes: [seqType.id],
        ])

        when:
        WorkflowVersion result = service.updateWorkflowVersion(updateDto)

        then:
        result.deprecatedDate == deprecatedDate

        and:
        1 * service.commentService.saveComment(workflowVersion, comment)
        1 * service.mergingCriteriaService.createDefaultMergingCriteria(seqType)
    }

    void "findAllByWorkflows, should return all workflow versions with the according workflow"() {
        given:
        Workflow workflow1 = createWorkflow()
        WorkflowVersion version1 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow1)])
        WorkflowVersion version2 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow1)])
        WorkflowVersion version3 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow1)])
        createWorkflowVersion()

        Workflow workflow2 = createWorkflow()
        WorkflowVersion version4 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow2)])

        expect:
        CollectionUtils.containSame(service.findAllByWorkflows([workflow1, workflow2]), [version1, version2, version3, version4])
    }
}
