/*
 * Copyright 2011-2021 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class WorkflowServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    private WorkflowService service

    void setupData(boolean useOutputAsInput = false) {
        service = new WorkflowService()
        service.jobService = Mock(JobService)
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            _ * lookupOtpWorkflowBean(_) >> Mock(OtpWorkflow) {
                1 * useOutputArtefactAlsoAsInputArtefact() >> useOutputAsInput
            }
        }
    }

    void "test, createRestartedWorkflow should create a copy of the old workflowArtefacts"() {
        given:
        setupData()

        WorkflowStep workflowStep = createWorkflowStep()
        workflowStep.workflowRun.state = WorkflowRun.State.FAILED
        WorkflowArtefact wa1 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun, outputRole: "asdf")
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun, outputRole: "qwertz")
        workflowStep.workflowRun.save()

        when:
        service.createRestartedWorkflow(workflowStep, false)

        then:
        noExceptionThrown()
        WorkflowArtefact.count() == 4
        WorkflowArtefact.findAllByProducedBy(workflowStep.workflowRun) == [wa1, wa2]
    }

    @Unroll
    void "test createRestartedWorkflow, if workflow use separate input artefacts, then create restarted workflow"() {
        given:
        setupData(false)

        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowRun oldRun = workflowStep.workflowRun
        WorkflowArtefact wa1 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: oldRun, outputRole: "asdf")
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: oldRun, outputRole: "qwertz")
        oldRun.state = WorkflowRun.State.FAILED
        oldRun.save()
        WorkflowArtefact wa3 = createWorkflowRunInputArtefact([
                workflowRun: oldRun,
        ]).workflowArtefact

        WorkflowRun wr2 = createWorkflowRun()
        createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa1)

        WorkflowRun wr3 = createWorkflowRun()
        createWorkflowRunInputArtefact(workflowRun: wr3, workflowArtefact: wa2)

        when:
        WorkflowRun newRun = service.createRestartedWorkflow(workflowStep, startDirectly)

        then:
        workflowStep.workflowRun.state == WorkflowRun.State.RESTARTED
        [wa1, wa2].every { it.state == WorkflowArtefact.State.FAILED }
        WorkflowArtefact.count == 5
        WorkflowRun.count == 4
        List<WorkflowArtefact> newWorkflowArtefacts = (WorkflowArtefact.all - [wa1, wa2, wa3])
        TestCase.assertContainSame(newRun.inputArtefacts, oldRun.inputArtefacts)
        TestCase.assertContainSame(newRun.outputArtefacts.values(), newWorkflowArtefacts)
        newWorkflowArtefacts.every { it.state == WorkflowArtefact.State.PLANNED_OR_RUNNING }
        newWorkflowArtefacts.every { it.producedBy == newRun }
        [wr2, wr3]*.inputArtefacts.every { Map<String, WorkflowArtefact> inputArtefacts ->
            inputArtefacts.values().every { it in newWorkflowArtefacts }
        }

        (startDirectly ? 1 : 0) * service.jobService.createNextJob(_) >> {
        }

        where:
        startDirectly << [true, false]
    }

    @Unroll
    void "test createRestartedWorkflow, if workflow use output artefacts also for input, then create restarted workflow with output artefact connected to the concrete artefact"() {
        given:
        setupData(true)

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

        when:
        WorkflowRun newRun = service.createRestartedWorkflow(workflowStep, startDirectly)

        then:
        workflowStep.workflowRun.state == WorkflowRun.State.RESTARTED
        newRun.state == WorkflowRun.State.PENDING
        wa.state == WorkflowArtefact.State.FAILED
        WorkflowArtefact.count == 2
        WorkflowRun.count == 3

        WorkflowArtefact newWorkflowArtefact = WorkflowArtefact.last()
        seqTrack.workflowArtefact == newWorkflowArtefact
        CollectionUtils.containSame(newRun.outputArtefacts.values(), [newWorkflowArtefact])

        newWorkflowArtefact.state == WorkflowArtefact.State.PLANNED_OR_RUNNING
        newWorkflowArtefact.producedBy == newRun

        wr2.inputArtefacts.values().every { it == newWorkflowArtefact }

        (startDirectly ? 1 : 0) * service.jobService.createNextJob(_) >> {
        }

        where:
        startDirectly << [
                true,
                false,
        ]
    }
}
