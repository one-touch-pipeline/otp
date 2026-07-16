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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflow.analysis.runyapsa.RunYapsaWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterJobHandlingService
import de.dkfz.tbi.otp.workflowExecution.wes.WesRunService

import java.time.LocalDate

@Rollback
@Integration
class WorkflowServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowService workflowService
    OtpWorkflowService otpWorkflowService
    JobService jobService

    static final String WORKFLOW_NAME = "WORKFLOW"

    void "createRestartedWorkflows, should create new WorkflowRun based on failed WorkflowRun"() {
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
        WorkflowRun newRun = workflowService.createRestartedWorkflows([workflowStep]).first()

        then:
        _ * workflowService.otpWorkflowService.lookupOtpWorkflowBean(_) >> otpWorkflow
        1 * otpWorkflow.createCopyOfArtefact(seqTrack) >> seqTrack
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
    }

    void "createRestartedWorkflow, when run has no workflow steps (e.g. was killed while still PENDING), then still create a new WorkflowRun"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([state: WorkflowRun.State.KILLED])
        workflowService.otpWorkflowService = Mock(OtpWorkflowService)

        when:
        WorkflowRun newRun = workflowService.createRestartedWorkflow(workflowRun)

        then:
        workflowRun.workflowSteps == null
        workflowRun.state == WorkflowRun.State.RESTARTED
        newRun.state == WorkflowRun.State.PENDING
        newRun.restartedFrom == workflowRun
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
        _ | allowedRefGenomesClosure                                     | supportedSeqTypesClosure
        _ | { it -> null }                                               | { it -> [] }
        _ | { it -> [createReferenceGenome(), createReferenceGenome()] } | { it -> [createSeqTypeSingle(), createSeqTypeSingle()] }
        _ | { it -> [] }                                                 | { it -> [createSeqTypeSingle(), createSeqTypeSingle()] }
        _ | { it -> [createReferenceGenome(), createReferenceGenome()] } | { it -> null }
    }

    void "test OtpWorkflow.reconnectDependencies, when current workflow is PanCancer and dependent workflow is Sophia"() {
        given:
        WorkflowService service = new WorkflowService()

        SophiaInstance sophiaInstance = SophiaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()

        Workflow panCancer = createWorkflow()
        WorkflowRun panCancerRun = createWorkflowRun(workflow: panCancer, state: WorkflowRun.State.FAILED)
        WorkflowStep workflowStep = createWorkflowStep(workflowRun: panCancerRun)
        WorkflowArtefact bamArtefact = createWorkflowArtefact(producedBy: panCancerRun)
        getBamFile(sophiaInstance).workflowArtefact = bamArtefact
        getBamFile(sophiaInstance).save(flush: true)

        Workflow sophia = createWorkflow()
        WorkflowRun sophiaRun = createWorkflowRun(workflow: sophia, state: WorkflowRun.State.FAILED)
        createWorkflowRunInputArtefact(workflowRun: sophiaRun, workflowArtefact: bamArtefact, role: role)
        WorkflowArtefact sophiaArtefact = createWorkflowArtefact(producedBy: sophiaRun)
        sophiaInstance.workflowArtefact = sophiaArtefact
        sophiaInstance.save(flush: true)

        service.otpWorkflowService = Mock(OtpWorkflowService) {
            lookupOtpWorkflowBean(panCancerRun) >> new PanCancerWorkflow()
            lookupOtpWorkflowBean(sophiaRun) >> new SophiaWorkflow()
        }

        when:
        WorkflowRun newRun = service.createRestartedWorkflow(workflowStep)

        then:
        newRun != panCancerRun
        WorkflowArtefact newWorkflowArtefact = CollectionUtils.exactlyOneElement(WorkflowArtefact.findAllByProducedBy(newRun))
        newWorkflowArtefact != bamArtefact
        newWorkflowArtefact.artefact != bamArtefact.artefact
        getBamFile(sophiaInstance) == newWorkflowArtefact.artefact.get()

        where:
        _ | getBamFile                                                 | role
        _ | { SophiaInstance instance -> instance.sampleType1BamFile } | AbstractAnalysisWorkflow.INPUT_TUMOR_BAM
        _ | { SophiaInstance instance -> instance.sampleType2BamFile } | AbstractAnalysisWorkflow.INPUT_CONTROL_BAM
    }

    @SuppressWarnings('UnnecessaryGetter')
    void "test OtpWorkflow.reconnectDependencies, when current workflow is #current and dependent workflow is #dependent"() {
        given:
        WorkflowService service = new WorkflowService()

        BamFilePairAnalysis dependentInstance = getDependentInstance()

        Workflow currentWorkflow = createWorkflow()
        WorkflowRun currentRun = createWorkflowRun(workflow: currentWorkflow, state: WorkflowRun.State.FAILED)
        WorkflowStep currentStep = createWorkflowStep(workflowRun: currentRun)
        WorkflowArtefact currentArtefact = createWorkflowArtefact(producedBy: currentRun)
        BamFilePairAnalysis currentInstance = getCurrentInstance(currentArtefact)

        currentInstance.samplePair.mergingWorkPackage1.bamFileInProjectFolder = currentInstance.sampleType1BamFile
        currentInstance.samplePair.mergingWorkPackage1.save(flush: true)
        currentInstance.samplePair.mergingWorkPackage2.bamFileInProjectFolder = currentInstance.sampleType2BamFile
        currentInstance.samplePair.mergingWorkPackage2.save(flush: true)

        Workflow dependentWorkflow = createWorkflow()
        WorkflowRun dependentRun = createWorkflowRun(workflow: dependentWorkflow, state: WorkflowRun.State.FAILED)
        createWorkflowRunInputArtefact(workflowRun: dependentRun, workflowArtefact: currentArtefact, role: inputRole)
        WorkflowArtefact dependentArtefact = createWorkflowArtefact(producedBy: dependentRun)
        dependentInstance.workflowArtefact = dependentArtefact
        dependentInstance.save(flush: true)

        service.otpWorkflowService = Mock(OtpWorkflowService) {
            lookupOtpWorkflowBean(currentRun) >> getCurrentOtpWorkflow()
            lookupOtpWorkflowBean(dependentRun) >> getDependentOtpWorkflow()
        }

        when:
        WorkflowRun newRun = service.createRestartedWorkflow(currentStep)

        then:
        newRun != currentRun
        WorkflowArtefact newWorkflowArtefact = CollectionUtils.exactlyOneElement(WorkflowArtefact.findAllByProducedBy(newRun))
        newWorkflowArtefact != dependentArtefact
        newWorkflowArtefact.artefact != dependentArtefact.artefact
        dependentRun.inputArtefacts.get(inputRole) == newWorkflowArtefact
        dependentRun.inputArtefacts.get(inputRole).artefact.get() == newWorkflowArtefact.artefact.get()

        where:
        _ | getCurrentInstance                                                                                                       | getDependentInstance                                                 | inputRole                   | getCurrentOtpWorkflow                                                                                             | getDependentOtpWorkflow    | current   | dependent
        _ | { workflowArtefact -> SophiaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(workflowArtefact: workflowArtefact) } | { AceseqDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles() }   | AceseqWorkflow.SOPHIA_INPUT | { new SophiaWorkflow(sophiaWorkFileService: Mock(SophiaWorkFileService) { constructInstanceName(_) >> "name" }) } | { new AceseqWorkflow() }   | "Sophia " | "ACEseq"
        _ | { workflowArtefact -> SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(workflowArtefact: workflowArtefact) }    | { RunYapsaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles() } | RunYapsaWorkflow.SNV_INPUT  | { new SnvWorkflow(snvWorkFileService: Mock(SnvWorkFileService) { constructInstanceName(_) >> "name" }) }          | { new RunYapsaWorkflow() } | "SNV"     | "runYapsa"
    }

    @SuppressWarnings('ClosureAsLastMethodParameter')
    void "killWorkflowRun, when run state is RUNNING_WES and step has multiple cluster jobs, then kill all cluster jobs"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                state        : WorkflowStep.State.SUCCESS,
                workflowRun  : createWorkflowRun([state: WorkflowRun.State.RUNNING_WES]),
        ])
        createClusterJob([workflowStep: workflowStep])
        createClusterJob([workflowStep: workflowStep])
        workflowStep.refresh()
        workflowService.clusterJobHandlingService = Mock(ClusterJobHandlingService)

        when:
        WorkflowRun result = workflowService.killWorkflowRun(workflowStep.workflowRun)

        then:
        1 * workflowService.clusterJobHandlingService.killClusterJobsInWorkflowStep(workflowStep)
        result.state == WorkflowRun.State.KILLED
    }

    void "killWorkflowRun, when run state is RUNNING_WES and step has multiple WES runs, then kill all WES runs"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                state        : WorkflowStep.State.SUCCESS,
                workflowRun  : createWorkflowRun([state: WorkflowRun.State.RUNNING_WES]),
        ])
        createWesRun([workflowStep: workflowStep])
        createWesRun([workflowStep: workflowStep])
        workflowStep.refresh()
        workflowService.wesRunService = Mock(WesRunService)

        when:
        WorkflowRun result = workflowService.killWorkflowRun(workflowStep.workflowRun)

        then:
        1 * workflowService.wesRunService.killWesRunsInWorkflowStep(workflowStep)
        result.state == WorkflowRun.State.KILLED
   }

    void "killWorkflowRun, when run state is RUNNING_OTP, then set run to KILLED without stopping the OTP job, so no subsequent jobs are started"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([state: WorkflowRun.State.RUNNING_OTP])
        WorkflowStep firstStep = createWorkflowStep([
                state      : WorkflowStep.State.RUNNING,
                workflowRun: workflowRun,
        ])

        when: "the run is killed while the first OTP job is still running"
        workflowService.killWorkflowRun(firstStep.workflowRun)

        then: "the run is KILLED, but the first OTP job has not stopped"
        workflowRun.refresh()
        firstStep.refresh()
        workflowRun.state == WorkflowRun.State.KILLED
        firstStep.state == WorkflowStep.State.RUNNING

        when: "the first OTP job finishes naturally and the scheduler tries to start the next job"
        firstStep.state = WorkflowStep.State.SUCCESS
        firstStep.save(flush: true)
        jobService.createNextJob(workflowRun)

        then: "no second step is started because the run is set to KILLED"
        workflowRun.refresh()
        workflowRun.workflowSteps.size() == 1
        firstStep.refresh()
        firstStep == workflowRun.workflowSteps.first()
        firstStep.state == WorkflowStep.State.SUCCESS
    }

    void "killWorkflowRun, when run state is PENDING, then set run to KILLED and do no start any subsequent jobs"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([state: WorkflowRun.State.PENDING])

        when: "the run is killed before the first OTP job is started"
        workflowService.killWorkflowRun(workflowRun)

        then: "the run is KILLED"
        workflowRun.refresh()
        workflowRun.state == WorkflowRun.State.KILLED

        when: "the scheduler tries to start the first job"
        jobService.createNextJob(workflowRun)

        then: "no step is started because the run is KILLED"
        workflowRun.refresh()
        workflowRun.workflowSteps.size() == 0
    }

    @Unroll
    void "killWorkflowRun, when run state is #runState, then no run is killed"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                state      : WorkflowStep.State.CREATED,
                workflowRun: createWorkflowRun([state: runState]),
        ])
        String expectedAssertionMessage = "WorkflowRun ${workflowStep.workflowRun} is not allowed to be killed"

        when:
        workflowService.killWorkflowRun(workflowStep.workflowRun)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains(expectedAssertionMessage)
        workflowStep.workflowRun.refresh()
        workflowStep.workflowRun.state == runState

        where:
        runState << [
                WorkflowRun.State.WAITING_FOR_USER,
                WorkflowRun.State.FAILED,
                WorkflowRun.State.FAILED_WAITING,
                WorkflowRun.State.FAILED_FINAL,
                WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
                WorkflowRun.State.SUCCESS,
                WorkflowRun.State.RESTARTED,
                WorkflowRun.State.LEGACY,
        ]
    }

    void "killWorkflowRun, when run state is RUNNING_WES and step has no cluster jobs or WES runs, then only set run to KILLED"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                state      : WorkflowStep.State.SUCCESS,
                workflowRun: createWorkflowRun([state: WorkflowRun.State.RUNNING_WES]),
        ])
        workflowService.clusterJobHandlingService = Mock(ClusterJobHandlingService) {
            0 * killClusterJobsInWorkflowStep(_)
        }
        workflowService.wesRunService = Mock(WesRunService) {
            0 * killWesRunsInWorkflowStep(_)
        }

        when:
        WorkflowRun result = workflowService.killWorkflowRun(workflowStep.workflowRun)

        then:
        result.state == WorkflowRun.State.KILLED
    }

    @Unroll
    void "killWorkflowRun, when project state is #projectState, then throw FileAccessForProjectNotAllowedException"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                state      : WorkflowStep.State.CREATED,
                workflowRun: createWorkflowRun([
                        state  : WorkflowRun.State.RUNNING_OTP,
                        project: createProject([state: projectState]),
                ]),
        ])

        String expectedExceptionMessage = "${workflowStep.workflowRun.project} is ${projectState.name().toLowerCase()} and ${workflowStep.workflowRun} cannot be killed"

        when:
        workflowService.killWorkflowRun(workflowStep.workflowRun)

        then:
        Exception e = thrown(FileAccessForProjectNotAllowedException)
        e.message.contains(expectedExceptionMessage)

        where:
        projectState << [Project.State.ARCHIVED, Project.State.DELETED]
    }
}
