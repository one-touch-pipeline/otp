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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactoryInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class WorkflowStateChangeServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, UserAndRoles {

    static private final List<?> INPUT_TABLE =
            [
                    [
                            "SeqTrack",
                            { DomainFactory.proxyCore.createSeqTrack() },
                            { true }
                    ],
                    [
                            "Fastqc",
                            { FastqcDomainFactoryInstance.INSTANCE.createFastqcProcessedFile() },
                            { true }
                    ],
                    [
                            "PanCancer",
                            { AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createBamFile() },
                            { it.withdrawn }
                    ],
                    [
                            "AlignmentPipelineFactory",
                            { AlignmentPipelineFactory.RoddyRnaFactoryInstance.INSTANCE.createBamFile() },
                            { it.withdrawn }
                    ],
                    [
                            "AlignmentPipelineFactory",
                            { AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile() },
                            { it.withdrawn }
                    ],
                    [
                            "SnvDomainFactory",
                            { SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles() },
                            { it.withdrawn }
                    ],
                    [
                            "IndelDomainFactory",
                            { IndelDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles() },
                            { it.withdrawn }
                    ],
                    [
                            "SophiaDomainFactory",
                            { SophiaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles() },
                            { it.withdrawn }
                    ],
                    [
                            "AceseqDomainFactory",
                            { AceseqDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles() },
                            { it.withdrawn }
                    ],
                    [
                            "RunYapsaDomainFactory",
                            { RunYapsaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles() },
                            { it.withdrawn }
                    ],
            ]*.asImmutable().asImmutable()

    WorkflowStateChangeService workflowStateChangeService

    private WorkflowStep workflowStep
    private WorkflowStepSkipMessage skippedMessage
    private WorkflowArtefact workflowArtefact

    void setupData() {
        workflowStep = createWorkflowStep()
        skippedMessage = new WorkflowStepSkipMessage(message: "asdf", category: WorkflowStepSkipMessage.Category.WORKFLOW_COVERAGE_REJECTION)
        workflowArtefact = createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "asdf")

        createUserAndRoles()
    }

    void "test changeStateToSkipped"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowStepSkipMessage skippedMessage = new WorkflowStepSkipMessage(message: "asdf", category: WorkflowStepSkipMessage.Category.WORKFLOW_COVERAGE_REJECTION)
        WorkflowArtefact wa1 = createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "asdf")

        WorkflowRun wr2 = createWorkflowRun(state: WorkflowRun.State.FAILED)
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.FAILED, producedBy: wr2, outputRole: "asdf")
        createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa1)

        WorkflowRun wr3 = createWorkflowRun(state: WorkflowRun.State.PENDING)
        WorkflowArtefact wa3 = createWorkflowArtefact(state: WorkflowArtefact.State.PLANNED_OR_RUNNING, producedBy: wr3, outputRole: "asdf")
        createWorkflowRunInputArtefact(workflowRun: wr3, workflowArtefact: wa2)

        when:
        workflowStateChangeService.changeStateToSkipped(workflowStep, skippedMessage)

        then:
        workflowStep.state == WorkflowStep.State.SKIPPED
        workflowStep.workflowRun.state == WorkflowRun.State.SKIPPED_MISSING_PRECONDITION
        workflowStep.workflowRun.skipMessage == skippedMessage
        wa1.state == WorkflowArtefact.State.SKIPPED

        wa2.state == WorkflowArtefact.State.FAILED
        wr2.state == WorkflowRun.State.FAILED

        wa3.state == WorkflowArtefact.State.SKIPPED
        wr3.state == WorkflowRun.State.SKIPPED_MISSING_PRECONDITION
        wr3.skipMessage == skippedMessage
    }

    void "changeStateToSkipped, if called workflow with artefact of #name, then the artefact should be withdrawn if supported"() {
        given:
        setupData()

        Artefact artefact = initClosure()
        artefact.workflowArtefact = workflowArtefact
        artefact.save(flush: true)

        when:
        doWithAuth(ADMIN) {
            workflowStateChangeService.changeStateToSkipped(workflowStep, skippedMessage)
        }

        then:
        checkClosure(artefact)

        where:
        [name, initClosure, checkClosure] << INPUT_TABLE
    }

    void "changeStateToSkipped, if called workflow having depending workflow with artefact of #name, then the artefact should be withdrawn if supported"() {
        given:
        setupData()

        WorkflowRun wr = createWorkflowRun(state: WorkflowRun.State.PENDING)
        WorkflowArtefact wa = createWorkflowArtefact(state: WorkflowArtefact.State.PLANNED_OR_RUNNING, producedBy: wr, outputRole: "asdf")
        createWorkflowRunInputArtefact(workflowRun: wr, workflowArtefact: workflowArtefact)

        Artefact artefact = initClosure()
        artefact.workflowArtefact = wa
        artefact.save(flush: true)

        when:
        doWithAuth(ADMIN) {
            workflowStateChangeService.changeStateToSkipped(workflowStep, skippedMessage)
        }

        then:
        checkClosure(artefact)

        where:
        [name, initClosure, checkClosure] << INPUT_TABLE
    }
}
