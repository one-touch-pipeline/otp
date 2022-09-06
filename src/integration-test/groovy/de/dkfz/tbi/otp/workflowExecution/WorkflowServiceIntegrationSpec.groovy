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
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class WorkflowServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowService workflowService

    @Unroll
    void "test createRestartedWorkflow, should create new WorkflowRun based on failed WorkflowRun and start it directly: #startDirectly"() {
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
        WorkflowRun newRun = workflowService.createRestartedWorkflow(workflowStep, startDirectly)

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
        CollectionUtils.containSame(newRun.outputArtefacts.values(), [newWorkflowArtefact])

        newWorkflowArtefact.state == WorkflowArtefact.State.PLANNED_OR_RUNNING
        newWorkflowArtefact.producedBy == newRun

        wr2.inputArtefacts.values().every { it == newWorkflowArtefact }

        where:
        startDirectly << [true, false]
    }
}
