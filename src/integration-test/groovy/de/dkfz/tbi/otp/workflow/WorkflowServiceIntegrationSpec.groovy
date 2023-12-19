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
package de.dkfz.tbi.otp.workflow

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.time.LocalDate

@Rollback
@Integration
class WorkflowServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {
    @Autowired
    WorkflowService workflowService

    static final String WORKFLOW_NAME = "WORKFLOW"

    void "getExactlyOneWorkflow, when a non deprecated workflow of the name exist, then return that workflow"() {
        given:
        createWorkflow()
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ])

        when:
        Workflow returnedWorkflow = workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        returnedWorkflow == workflow
    }

    void "getExactlyOneWorkflow, when no workflow of the name exists, then throw an exception"() {
        createWorkflow()

        when:
        workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        thrown(AssertionError)
    }

    void "getExactlyOneWorkflow, when the only workflow with this name is deprecated, then throw an exception"() {
        createWorkflow([
                name          : WORKFLOW_NAME,
                deprecatedDate: LocalDate.now(),
        ])

        when:
        workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        thrown(AssertionError)
    }

    void "findAllSeqTypesContainedInVersions, should return all seq types that are contained in the versions of a workflow"() {
        given:
        Workflow workflow = createWorkflow()
        SeqType seqType1 = createSeqType()
        SeqType seqType2 = createSeqType()
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow),
                supportedSeqTypes : [seqType1],
        ])
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow),
                supportedSeqTypes : [seqType1, seqType2],
        ])
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow),
                supportedSeqTypes : [seqType2],
        ])
        createWorkflowVersion()

        expect:
        CollectionUtils.containSame(workflowService.getSupportedSeqTypesOfVersions(workflow), [seqType1, seqType2])
    }

    void "findAllSeqTypesContainedInVersions, should return all seq types that are contained in the versions of a list of workflow"() {
        given:
        Workflow workflow1 = createWorkflow()
        SeqType seqType1 = createSeqType()
        SeqType seqType2 = createSeqType()
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow1),
                supportedSeqTypes : [seqType1],
        ])
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow1),
                supportedSeqTypes : [seqType1, seqType2],
        ])
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow1),
                supportedSeqTypes : [seqType2],
        ])
        createWorkflowVersion()
        Workflow workflow2 = createWorkflow()
        SeqType seqType3 = createSeqType()
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow2),
                supportedSeqTypes : [seqType3],
        ])
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow2),
                supportedSeqTypes : [seqType3, seqType2],
        ])
        createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow2),
                supportedSeqTypes : [seqType2],
        ])
        createWorkflowVersion()

        expect:
        CollectionUtils.containSame(workflowService.getSupportedSeqTypesOfVersions([workflow1, workflow2]), [seqType1, seqType2, seqType3])
    }

    @Unroll
    void "findAllSeqTypesContainedInVersions, should return an empty list if its called with #name"() {
        given:
        SeqType seqType1 = createSeqType()
        createWorkflowVersion([
                supportedSeqTypes: [seqType1],
        ])

        expect:
        workflowService.getSupportedSeqTypesOfVersions(input) == []

        where:
        name            | input
        "an empty list" | []
        "null"          | null
    }

    void "findAllFastqcWorkflows, should return all the fastqc Workflows"() {
        given:
        Workflow workflow1 = createWorkflow([
                beanName: 'bean2',
                name    : WesFastQcWorkflow.WORKFLOW,
        ])
        Workflow workflow2 = createWorkflow([
                beanName: 'bean1',
                name    : BashFastQcWorkflow.WORKFLOW,
        ])
        createWorkflow()

        expect:
        workflowService.findAllFastqcWorkflows() == [workflow2, workflow1]
    }
}
