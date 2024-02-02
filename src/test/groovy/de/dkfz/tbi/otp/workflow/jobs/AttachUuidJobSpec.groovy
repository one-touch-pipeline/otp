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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.*
import de.dkfz.tbi.otp.workflowExecution.*

class AttachUuidJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    void "execute, if no workFolder exist, then create and attach one"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                workFolder: null,
        ])
        WorkflowStep workflowStep = createWorkflowStep(
                workflowRun: workflowRun,
        )
        BaseFolder baseFolder = createBaseFolder()
        WorkFolder workFolder = createWorkFolder([
                baseFolder: baseFolder,
        ])

        AttachUuidJob job = new AttachUuidJob()
        job.filestoreService = Mock(FilestoreService) {
            1 * findAnyWritableBaseFolder() >> baseFolder
            1 * createWorkFolder(baseFolder) >> workFolder
            1 * attachWorkFolder(workflowRun, workFolder) >> {
                workflowRun.workFolder = workFolder
            }
            0 * _
        }
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep, "Attach new workfolder: ${workFolder}")
            0 * _
        }
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToSuccess(workflowStep)
            0 * _
        }

        when:
        job.execute(workflowStep)

        then:
        workflowRun.workFolder == workFolder
    }

    void "execute, if no workFolder exist, then create and attach a workFolder"() {
        given:
        BaseFolder baseFolder = createBaseFolder()
        WorkFolder workFolder = createWorkFolder([
                baseFolder: baseFolder,
        ])
        WorkflowRun workflowRun = createWorkflowRun([
                workFolder: workFolder,
        ])
        WorkflowStep workflowStep = createWorkflowStep(
                workflowRun: workflowRun,
        )

        AttachUuidJob job = new AttachUuidJob()
        job.filestoreService = Mock(FilestoreService) {
            0 * _
        }
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep, "Reuse existing attached workfolder: ${workFolder}")
            0 * _
        }
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToSuccess(workflowStep)
            0 * _
        }

        when:
        job.execute(workflowStep)

        then:
        workflowRun.workFolder == workFolder
    }
}
