/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils

class WorkflowServiceSpec extends Specification implements ServiceUnitTest<WorkflowService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                WorkflowRun,
                WorkflowRunInputArtefact,
        ]
    }

    void "test createRestartedWorkflow"() {
        given:
        service.jobService = Mock(JobService)

        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowArtefact wa1 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun, outputRole: "asdf")
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun, outputRole: "qwertz")
        workflowStep.workflowRun.state = WorkflowRun.State.FAILED
        workflowStep.workflowRun.save(flush: true)

        WorkflowRun wr2 = createWorkflowRun()
        createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa1)

        WorkflowRun wr3 = createWorkflowRun()
        createWorkflowRunInputArtefact(workflowRun: wr3, workflowArtefact: wa2)

        when:
        service.createRestartedWorkflow(workflowStep, startDirectly)

        then:
        workflowStep.workflowRun.state == WorkflowRun.State.RESTARTED
        [wa1, wa2].every { it.state == WorkflowArtefact.State.FAILED }
        WorkflowArtefact.count == 4
        WorkflowRun.count == 4
        List<WorkflowArtefact> newWorkflowArtefacts = (WorkflowArtefact.all - [wa1, wa2])
        WorkflowRun newRun = (WorkflowRun.all - [workflowStep.workflowRun, wr2, wr3]).first()
        CollectionUtils.containSame(newRun.outputArtefacts.values(), newWorkflowArtefacts)
        newWorkflowArtefacts.every { it.state == WorkflowArtefact.State.PLANNED_OR_RUNNING }
        newWorkflowArtefacts.every { it.producedBy == newRun }
        [wr2, wr3]*.inputArtefacts.every { Map<String, WorkflowArtefact> inputArtefacts ->
            inputArtefacts.values().every { it in newWorkflowArtefacts }
        }

        (startDirectly ? 1 : 0) * service.jobService.createNextJob(_) >> { }

        where:
        startDirectly << [true, false]
    }
}
