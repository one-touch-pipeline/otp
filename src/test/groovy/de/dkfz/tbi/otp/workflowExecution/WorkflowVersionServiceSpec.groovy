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
        WorkflowVersion workflowVersion1 = createWorkflowVersion([workflow: workflow])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([workflow: workflow])
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

    void "findAllByWorkflowSeqTypeAndReferenceGenome, should find the correct workflow versions also for null"() {
        given:
        Workflow workflow = createWorkflow()
        SeqType seqType1 = createSeqType()
        SeqType seqType2 = createSeqType()
        Set<SeqType> seqTypes = [seqType1, seqType2] as Set
        ReferenceGenome refGenome1 = createReferenceGenome()
        ReferenceGenome refGenome2 = createReferenceGenome()
        Set<ReferenceGenome> refGenomes = [refGenome1, refGenome2] as Set
        WorkflowVersion workflowVersion1 = createWorkflowVersion([
                workflow               : workflow,
                supportedSeqTypes      : seqTypes,
                allowedReferenceGenomes: refGenomes,
        ])
        WorkflowVersion workflowVersion2 = createWorkflowVersion([workflow: workflow])
        WorkflowVersion workflowVersion3 = createWorkflowVersion([workflow: workflow, supportedSeqTypes: seqTypes])
        WorkflowVersion workflowVersion4 = createWorkflowVersion([workflow: workflow, allowedReferenceGenomes: refGenomes])
        WorkflowVersion workflowVersion5 = createWorkflowVersion([supportedSeqTypes: seqTypes])
        WorkflowVersion workflowVersion6 = createWorkflowVersion([supportedSeqTypes: seqTypes, allowedReferenceGenomes: refGenomes])
        WorkflowVersion workflowVersion7 = createWorkflowVersion([allowedReferenceGenomes: refGenomes])

        expect:
        List<WorkflowVersion> result1 = service.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, seqType1, refGenome1)
        CollectionUtils.containSame(result1, [workflowVersion1])
        List<WorkflowVersion> result2 = service.findAllByWorkflowSeqTypeAndReferenceGenome(null, seqType1, refGenome1)
        CollectionUtils.containSame(result2, [workflowVersion6, workflowVersion1])
        List<WorkflowVersion> result3 = service.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, null, refGenome1)
        CollectionUtils.containSame(result3, [workflowVersion4, workflowVersion1])
        List<WorkflowVersion> result4 = service.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, seqType2, null)
        CollectionUtils.containSame(result4, [workflowVersion3, workflowVersion1])
        List<WorkflowVersion> result5 = service.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, null, null)
        CollectionUtils.containSame(result5, [workflowVersion3, workflowVersion4, workflowVersion2, workflowVersion1])
        List<WorkflowVersion> result6 = service.findAllByWorkflowSeqTypeAndReferenceGenome(null, seqType1, null)
        CollectionUtils.containSame(result6, [workflowVersion1, workflowVersion3, workflowVersion5, workflowVersion6])
        List<WorkflowVersion> result7 = service.findAllByWorkflowSeqTypeAndReferenceGenome(null, null, refGenome2)
        CollectionUtils.containSame(result7, [workflowVersion1, workflowVersion4, workflowVersion6, workflowVersion7])
    }

    void "findAllByWorkflows, should return all workflow versions with the according workflow"() {
        given:
        Workflow workflow1 = createWorkflow()
        WorkflowVersion version1 = createWorkflowVersion([workflow: workflow1])
        WorkflowVersion version2 = createWorkflowVersion([workflow: workflow1])
        WorkflowVersion version3 = createWorkflowVersion([workflow: workflow1])
        createWorkflowVersion()

        Workflow workflow2 = createWorkflow()
        WorkflowVersion version4 = createWorkflowVersion([workflow: workflow2])

        expect:
        CollectionUtils.containSame(service.findAllByWorkflows([workflow1, workflow2]), [version1, version2, version3, version4])
    }
}
