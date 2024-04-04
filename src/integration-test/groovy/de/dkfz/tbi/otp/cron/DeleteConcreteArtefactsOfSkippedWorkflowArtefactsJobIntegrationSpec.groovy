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
package de.dkfz.tbi.otp.cron

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

@Rollback
@Integration
class DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJobIntegrationSpec extends Specification implements DomainFactoryCore, WorkflowSystemDomainFactory {

    void "test wrappedExecute, with correct state and no dependent workflow artefact, deletes artefact"() {
        given:
        DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob job = new DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob()

        WorkflowArtefact wa = createWorkflowArtefact(state: WorkflowArtefact.State.SKIPPED)
        createSeqTrack(workflowArtefact: wa)

        when:
        job.wrappedExecute()

        then:
        WorkflowArtefact.all*.artefact*.orElse(null).findAll().empty
    }

    void "test wrappedExecute, with correct state and dependent workflow artefact without artefact, deletes artefact"() {
        given:
        DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob job = new DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob()

        WorkflowArtefact wa = createWorkflowArtefact(state: WorkflowArtefact.State.SKIPPED)
        createSeqTrack(workflowArtefact: wa)

        WorkflowRun wr = createWorkflowRun()
        createWorkflowRunInputArtefact(workflowArtefact: wa, workflowRun: wr)
        createWorkflowArtefact(producedBy: wr)

        when:
        job.wrappedExecute()

        then:
        WorkflowArtefact.all*.artefact*.orElse(null).findAll().empty
    }

    void "test wrappedExecute, with incorrect state, doesn't delete artefact"() {
        given:
        DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob job = new DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob()

        WorkflowArtefact wa = createWorkflowArtefact(state: state)
        SeqTrack artefact = createSeqTrack(workflowArtefact: wa)

        when:
        job.wrappedExecute()

        then:
        artefact in WorkflowArtefact.all*.artefact*.orElse(null)

        where:
        state                                     || _
        WorkflowArtefact.State.PLANNED_OR_RUNNING || _
        WorkflowArtefact.State.SUCCESS            || _
        WorkflowArtefact.State.FAILED             || _
    }

    void "test wrappedExecute, with correct state and dependent workflow artefact with artefact, doesn't delete artefact"() {
        given:
        DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob job = new DeleteConcreteArtefactsOfSkippedWorkflowArtefactsJob()

        WorkflowArtefact wa = createWorkflowArtefact(state: WorkflowArtefact.State.SKIPPED)
        SeqTrack artefact = createSeqTrack(workflowArtefact: wa)

        WorkflowRun wr = createWorkflowRun()
        createWorkflowRunInputArtefact(workflowArtefact: wa, workflowRun: wr)
        WorkflowArtefact wa2 = createWorkflowArtefact(producedBy: wr)
        createSeqTrack(workflowArtefact: wa2)

        when:
        job.wrappedExecute()

        then:
        artefact in WorkflowArtefact.all*.artefact*.orElse(null)
    }
}
