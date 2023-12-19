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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class WorkflowVersionServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {
    WorkflowVersionService workflowVersionService

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
                apiVersion             : createWorkflowApiVersion(workflow: workflow),
                supportedSeqTypes      : seqTypes,
                allowedReferenceGenomes: refGenomes,
        ])
        WorkflowVersion workflowVersion2 = createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: workflow))
        WorkflowVersion workflowVersion3 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow), supportedSeqTypes: seqTypes])
        WorkflowVersion workflowVersion4 = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow), allowedReferenceGenomes: refGenomes])
        WorkflowVersion workflowVersion5 = createWorkflowVersion(supportedSeqTypes: seqTypes)
        WorkflowVersion workflowVersion6 = createWorkflowVersion([supportedSeqTypes: seqTypes, allowedReferenceGenomes: refGenomes])
        WorkflowVersion workflowVersion7 = createWorkflowVersion(allowedReferenceGenomes: refGenomes)

        expect:
        List<WorkflowVersion> result1 = workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, seqType1, refGenome1)
        CollectionUtils.containSame(result1, [workflowVersion1])
        List<WorkflowVersion> result2 = workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(null, seqType1, refGenome1)
        CollectionUtils.containSame(result2, [workflowVersion6, workflowVersion1])
        List<WorkflowVersion> result3 = workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, null, refGenome1)
        CollectionUtils.containSame(result3, [workflowVersion4, workflowVersion1])
        List<WorkflowVersion> result4 = workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, seqType2, null)
        CollectionUtils.containSame(result4, [workflowVersion3, workflowVersion1])
        List<WorkflowVersion> result5 = workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(workflow, null, null)
        CollectionUtils.containSame(result5, [workflowVersion3, workflowVersion4, workflowVersion2, workflowVersion1])
        List<WorkflowVersion> result6 = workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(null, seqType1, null)
        CollectionUtils.containSame(result6, [workflowVersion1, workflowVersion3, workflowVersion5, workflowVersion6])
        List<WorkflowVersion> result7 = workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(null, null, refGenome2)
        CollectionUtils.containSame(result7, [workflowVersion1, workflowVersion4, workflowVersion6, workflowVersion7])
    }
}
