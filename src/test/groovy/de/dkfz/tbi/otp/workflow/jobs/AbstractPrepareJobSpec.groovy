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
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path
import java.nio.file.Paths

class AbstractPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingPriority,
                Project,
                Realm,
                Workflow,
                WorkflowRun,
        ]
    }

    void "test execute"() {
        given:
        Path workDirectory = Paths.get("/test")
        Path link, target
        AbstractPrepareJob job = Spy(AbstractPrepareJob)
        job.fileService = Mock(FileService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        job.execute(workflowStep)

        then:
        1 * job.buildWorkDirectoryPath(workflowStep) >> workDirectory
        1 * job.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workDirectory, _, workflowStep.workflowRun.project.unixGroup) >> null
        workflowStep.workflowRun.workDirectory == workDirectory.toString()

        1 * job.generateMapForLinking(workflowStep) >> [new LinkEntry(link, target)]
        1 * job.fileService.createLink(target, link, _, CreateLinkOption.DELETE_EXISTING_FILE) >> null

        1 * job.doFurtherPreparation(workflowStep) >> null

        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }
}
