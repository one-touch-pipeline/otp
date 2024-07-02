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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow

import java.time.LocalDate

@Rollback
@Integration
class WorkflowServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowService workflowService
    OtpWorkflowService otpWorkflowService

    static final String WORKFLOW_NAME = "WORKFLOW"

    @Unroll
    void "createRestartedWorkflows, should create new WorkflowRun based on failed WorkflowRun and start it directly: #startDirectly"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        state: WorkflowRun.State.FAILED,
                ]),
        ])
        WorkflowArtefact wa = createWorkflowArtefact([
                state     : WorkflowArtefact.State.FAILED,
                producedBy: workflowStep.workflowRun,
        ])
        SeqTrack seqTrack = createSeqTrack([
                workflowArtefact: wa,
        ])

        WorkflowRun wr2 = createWorkflowRun()
        createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa)

        workflowService.jobService = Mock(JobService)
        workflowService.otpWorkflowService = Mock(OtpWorkflowService)
        OtpWorkflow otpWorkflow = Mock(OtpWorkflow)

        when:
        WorkflowRun newRun = workflowService.createRestartedWorkflows([workflowStep], startDirectly).first()

        then:
        _ * workflowService.otpWorkflowService.lookupOtpWorkflowBean(_) >> otpWorkflow
        1 * otpWorkflow.createCopyOfArtefact(seqTrack) >> seqTrack
        (startDirectly ? 1 : 0) * workflowService.jobService.createNextJob(_)
        _ * otpWorkflow.reconnectDependencies(_, _)

        and:
        workflowStep.workflowRun.state == WorkflowRun.State.RESTARTED
        newRun.state == WorkflowRun.State.PENDING
        newRun.workDirectory != null
        wa.state == WorkflowArtefact.State.FAILED
        WorkflowArtefact.count == 2
        WorkflowRun.count == 3

        WorkflowArtefact newWorkflowArtefact = WorkflowArtefact.last()
        TestCase.assertContainSame(newRun.outputArtefacts.values(), [newWorkflowArtefact])

        newWorkflowArtefact.state == WorkflowArtefact.State.PLANNED_OR_RUNNING
        newWorkflowArtefact.producedBy == newRun

        wr2.inputArtefacts.values().every { it == newWorkflowArtefact }

        where:
        startDirectly << [true, false]
    }

    void "findAllAlignmentWorkflows, should return all the alignment workflows"() {
        given:
        Workflow workflow = createWorkflow([beanName: 'rnaAlignmentWorkflow'])
        createWorkflow()

        expect:
        workflowService.findAllAlignmentWorkflows() == [workflow]
    }

    void "findAllAlignmentWorkflowsForSeqType, should return all the alignment workflows for a given seqtype"() {
        given: "alignment workflow for given seqtype"
        SeqType seqType = DomainFactory.createRnaPairedSeqType()
        Workflow workflow = createWorkflow(beanName: "rnaAlignmentWorkflow")
        createWorkflowVersion(supportedSeqTypes: [seqType], apiVersion: createWorkflowApiVersion(workflow: workflow))

        and: "other workflows that are not alignment workflows"
        createWorkflowVersion(supportedSeqTypes: [seqType], apiVersion: createWorkflowApiVersion(workflow: createWorkflow()))

        and: "other alignment workflows that do not have a connection to the seqtype"
        createWorkflowVersion(supportedSeqTypes: [DomainFactory.createWholeGenomeSeqType()], apiVersion: createWorkflowApiVersion(workflow: createWorkflow(beanName: "panCancerWorkflow")))

        expect:
        workflowService.findAlignmentWorkflowsForSeqType(seqType) == workflow
    }

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
                apiVersion       : createWorkflowApiVersion(workflow: workflow),
                supportedSeqTypes: [seqType1],
        ])
        createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: workflow),
                supportedSeqTypes: [seqType1, seqType2],
        ])
        createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: workflow),
                supportedSeqTypes: [seqType2],
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
                apiVersion       : createWorkflowApiVersion(workflow: workflow1),
                supportedSeqTypes: [seqType1],
        ])
        createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: workflow1),
                supportedSeqTypes: [seqType1, seqType2],
        ])
        createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: workflow1),
                supportedSeqTypes: [seqType2],
        ])
        createWorkflowVersion()
        Workflow workflow2 = createWorkflow()
        SeqType seqType3 = createSeqType()
        createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: workflow2),
                supportedSeqTypes: [seqType3],
        ])
        createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: workflow2),
                supportedSeqTypes: [seqType3, seqType2],
        ])
        createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: workflow2),
                supportedSeqTypes: [seqType2],
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

    void "findAllAnalysisWorkflows, should return all existing analysis workflows"() {
        given:
        Workflow analysisWorkflow1 = createWorkflow([beanName: SnvWorkflow.simpleName.uncapitalize()])
        Workflow analysisWorkflow2 = createWorkflow([beanName: IndelWorkflow.simpleName.uncapitalize()])
        Workflow analysisWorkflow3 = createWorkflow([beanName: AceseqWorkflow.simpleName.uncapitalize()])
        createWorkflow([beanName: RnaAlignmentWorkflow.simpleName.uncapitalize()])

        expect:
        TestCase.assertContainSame(workflowService.findAllAnalysisWorkflows(), [analysisWorkflow1, analysisWorkflow2, analysisWorkflow3])
    }

    void "isAlignment, should return true for alignment workflows"() {
        given:
        workflowService.otpWorkflowService = otpWorkflowService
        Workflow alignmentWorkflow = createWorkflow([beanName: RnaAlignmentWorkflow.simpleName.uncapitalize()])

        expect:
        workflowService.isAlignment(alignmentWorkflow)
    }

    void "isAlignment, should return false for non alignment workflows"() {
        given:
        Workflow analysisWorkflow = createWorkflow([beanName: SnvWorkflow.simpleName.uncapitalize()])

        expect:
        !workflowService.isAlignment(analysisWorkflow)
    }

    void "disabling workflow should be idempotent"() {
        given:
        Workflow workflow = createWorkflow()

        expect:
        workflow.enabled

        when:
        workflowService.disableWorkflow(workflow)

        then:
        !workflow.enabled

        when:
        workflowService.disableWorkflow(workflow)

        then:
        !workflow.enabled
    }

    void "enabling workflow should be idempotent"() {
        given:
        Workflow workflow = createWorkflow([enabled: false])

        expect:
        !workflow.enabled

        when:
        workflowService.enableWorkflow(workflow)

        then:
        workflow.enabled

        when:
        workflowService.enableWorkflow(workflow)

        then:
        workflow.enabled
    }

    void "findAllByDeprecatedDateIsNull, should return all workflow with deprecated date is null"() {
        given:
        Workflow workflow1 = createWorkflow()
        Workflow workflow2 = createWorkflow()
        createWorkflow([deprecatedDate: LocalDate.now()])

        expect:
        TestCase.assertContainSame(workflowService.findAllByDeprecatedDateIsNull(), [workflow1, workflow2])
    }

    @Unroll
    void "updateWorkflow should update given workflow with dto data"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowVersion workflowVersion = createWorkflowVersion([apiVersion: createWorkflowApiVersion([workflow: workflow])])
        Set<ReferenceGenome> allowedRefGenomes = allowedRefGenomesClosure()
        Set<SeqType> supportedSeqTypes = supportedSeqTypesClosure()
        UpdateWorkflowDto updateWorkflowDto = new UpdateWorkflowDto([
                id                  : workflow.id,
                priority            : 12,
                maxParallelWorkflows: 3,
                enabled             : true,
                defaultVersion      : workflowVersion,
                allowedRefGenomes   : (allowedRefGenomes ? allowedRefGenomes*.id : allowedRefGenomes) as List,
                supportedSeqTypes   : (supportedSeqTypes ? supportedSeqTypes*.id : supportedSeqTypes) as List,
        ])

        when:
        workflowService.updateWorkflow(updateWorkflowDto)

        then:
        workflow.priority == updateWorkflowDto.priority
        workflow.maxParallelWorkflows == updateWorkflowDto.maxParallelWorkflows
        workflow.enabled == updateWorkflowDto.enabled
        workflow.defaultVersion == updateWorkflowDto.defaultVersion
        workflow.defaultReferenceGenomesForWorkflowVersions == (allowedRefGenomes?.size() && allowedRefGenomes.size() > 0 ? allowedRefGenomes : null)
        workflow.defaultSeqTypesForWorkflowVersions == (supportedSeqTypes?.size() && supportedSeqTypes.size() > 0 ? supportedSeqTypes : null)

        where:
        allowedRefGenomesClosure                                     | supportedSeqTypesClosure
        { it -> null }                                               | { it -> [] }
        { it -> [createReferenceGenome(), createReferenceGenome()] } | { it -> [createSeqTypeSingle(), createSeqTypeSingle()] }
        { it -> [] }                                                 | { it -> [createSeqTypeSingle(), createSeqTypeSingle()] }
        { it -> [createReferenceGenome(), createReferenceGenome()] } | { it -> null }
    }
}
