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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

@Rollback
@Integration
class CleanupWorkFoldersJobIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    CleanupWorkFoldersJob job

    void setupData() {
        job = new CleanupWorkFoldersJob()
        job.filestoreService = new FilestoreService()
        job.filestoreService.fileSystemService = new TestFileSystemService()
        job.filestoreService.fileService = new FileService()
    }

    @Unroll
    void "wrappedExecute, should delete"() {
        given:
        setupData()
        WorkflowRun finalFailed = createWorkflowRun(state: WorkflowRun.State.FAILED_FINAL, workFolder: createWorkFolder())
        WorkflowRun alreadyDeleted = createWorkflowRun(state: WorkflowRun.State.FAILED_FINAL, workFolder: createWorkFolder(deleted: true))
        WorkflowRun restarted = createWorkflowRun(state: WorkflowRun.State.RESTARTED, workFolder: createWorkFolder())
        createWorkflowRun(state: WorkflowRun.State.RESTARTED, workFolder: null)
        createWorkflowRun(state: WorkflowRun.State.LEGACY, workFolder: null)
        createWorkflowRun(state: WorkflowRun.State.PENDING, workFolder: createWorkFolder())

        expect:
        CollectionUtils.containSame(WorkFolder.findAllByDeleted(true), [alreadyDeleted.workFolder])

        when:
        job.wrappedExecute()

        then:
        TestCase.assertContainSame(WorkFolder.findAllByDeleted(true), [finalFailed, restarted, alreadyDeleted]*.workFolder)
    }
}
