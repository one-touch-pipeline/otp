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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class AbstractLinkJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    void "test execute"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        Path target1 = Paths.get("target1")
        Path target2 = Paths.get("target2")
        Path link1 = Paths.get("link1")
        Path link2 = Paths.get("link2")
        AbstractLinkJob job = Spy(AbstractLinkJob)
        job.fileService = Mock(FileService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.logService = Mock(LogService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.getLinkMap(workflowStep) >> { [new LinkEntry(target: target1, link: link1), new LinkEntry(target: target2, link: link2)] }
        1 * job.fileService.createLink(link1, target1, workflowStep.workflowRun.project.realm, CreateLinkOption.DELETE_EXISTING_FILE)
        1 * job.fileService.createLink(link2, target2, workflowStep.workflowRun.project.realm, CreateLinkOption.DELETE_EXISTING_FILE)
        1 * job.doFurtherWork(workflowStep) >> null
        1 * job.saveResult(workflowStep) >> null
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }
}
