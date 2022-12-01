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

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

class WorkflowCreatorSchedulerSpec extends Specification implements ServiceUnitTest<WorkflowCreatorScheduler>, DataTest, DomainFactoryCore, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MetaDataFile,
        ]
    }

    void "createWorkflows, if system runs and nextWaitingWorkflow return null, then do not call createJob "() {
        given:
        WorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        List<WorkflowRun> runs = [ createWorkflowRun() ]

        when:
        scheduler.scheduleCreateWorkFlow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true

        1 * scheduler.fastqImportInstanceService.waiting() >> fastqImportInstance
        1 * scheduler.metaDataFileService.getByFastqImportInstance(_) >> metaDataFile
        2 * scheduler.fastqImportInstanceService.updateState(_, _)

        1 * scheduler.fastqImportInstanceService.findCountWithWaitingState() >> 1
        1 * scheduler.dataInstallationInitializationService.createWorkflowRuns(_) >> runs
        1 * scheduler.allDecider.decide(_, _)
        1 * scheduler.notificationCreator.sendWorkflowCreateSuccessMail(_)
    }

    void "Run createWorkflows and it throws an exception, it should catches the exception, set the correct state, and send E-Mail to ticketing system"() {
        given:
        WorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        List<WorkflowRun> runs = [ createWorkflowRun() ]

        OtpRuntimeException otpRuntimeException = new OtpRuntimeException("Decider throws exceptions")

        when:
        scheduler.scheduleCreateWorkFlow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true

        1 * scheduler.fastqImportInstanceService.waiting() >> fastqImportInstance
        1 * scheduler.metaDataFileService.getByFastqImportInstance(_) >> metaDataFile
        2 * scheduler.fastqImportInstanceService.updateState(_, _)

        1 * scheduler.fastqImportInstanceService.findCountWithWaitingState() >> 1
        1 * scheduler.dataInstallationInitializationService.createWorkflowRuns(_) >> runs
        1 * scheduler.allDecider.decide(_, _) >> { throw otpRuntimeException }
        1 * scheduler.notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, otpRuntimeException)
    }

    private WorkflowCreatorScheduler createWorkflowCreatorScheduler() {
        return new WorkflowCreatorScheduler([
                allDecider                           : Mock(AllDecider),
                dataInstallationInitializationService: Mock(DataInstallationInitializationService),
                fastqImportInstanceService           : Mock(FastqImportInstanceService),
                metaDataFileService                  : Mock(MetaDataFileService),
                notificationCreator                  : Mock(NotificationCreator),
                workflowSystemService                : Mock(WorkflowSystemService),
        ])
    }
}
