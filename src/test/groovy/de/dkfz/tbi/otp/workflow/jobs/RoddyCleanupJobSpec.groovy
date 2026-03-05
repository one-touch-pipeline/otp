/*
 * Copyright 2011-2026 The OTP authors
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
import spock.lang.TempDir

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class RoddyCleanupJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    private RoddyCleanupJob buildJob() {
        RoddyCleanupJob job = new RoddyCleanupJob()
        job.fileService = Mock(FileService)
        job.fileSystemService = new TestFileSystemService()
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        return job
    }

    private WorkflowStep buildStep() {
        return createWorkflowStep(
                workflowRun: createWorkflowRun(workDirectory: tempDir.toString())
        )
    }

    void "test execute, .roddyExecutionStore exists and gets deleted"() {
        given:
        RoddyCleanupJob job = buildJob()
        WorkflowStep workflowStep = buildStep()

        Files.createDirectories(tempDir.resolve('.roddyExecutionStore'))

        when:
        job.execute(workflowStep)

        then:
        1 * job.fileService.deleteDirectoryRecursively(tempDir.resolve('.roddyExecutionStore'))
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @SuppressWarnings('ClosureAsLastMethodParameter')
    void "test execute, neither roddyExecutionStore nor .roddyExecutionStore exist"() {
        given:
        RoddyCleanupJob job = buildJob()
        WorkflowStep workflowStep = buildStep()

        when:
        job.execute(workflowStep)

        then:
        // deleteDirectoryRecursively has a File.Exist(path) check  - therefore this method will always be called
        1 * job.fileService.deleteDirectoryRecursively(tempDir.resolve('.roddyExecutionStore'))
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        0 * job.fileService.deleteDirectoryRecursively({ it.endsWith('analysisTools') })
    }

    void "test execute, roddyExecutionStore exists with analysisTools containing nested files"() {
        given:
        RoddyCleanupJob job = buildJob()
        WorkflowStep workflowStep = buildStep()

        Path execDir = Files.createDirectories(tempDir.resolve('roddyExecutionStore').resolve('exec_2024-01-01_analysis'))
        Files.createDirectories(execDir.resolve('analysisTools').resolve('exomePipeline'))

        when:
        job.execute(workflowStep)

        then:
        1 * job.fileService.deleteDirectoryRecursively(execDir.resolve('analysisTools'))
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "test execute, roddyExecutionStore contains multiple directories containing an analysis directory"() {
        given:
        RoddyCleanupJob job = buildJob()
        WorkflowStep workflowStep = buildStep()

        Path execDir1 = Files.createDirectories(tempDir.resolve('roddyExecutionStore').resolve('exec_2024-01-01_analysis'))
        Path execDir2 = Files.createDirectories(tempDir.resolve('roddyExecutionStore').resolve('exec_2024-01-02_analysis'))

        Files.createDirectories(execDir1.resolve('analysisTools'))
        Files.createDirectories(execDir2.resolve('analysisTools'))

        when:
        job.execute(workflowStep)

        then:
        1 * job.fileService.deleteDirectoryRecursively(execDir1.resolve('analysisTools'))
        1 * job.fileService.deleteDirectoryRecursively(execDir2.resolve('analysisTools'))
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "test execute, roddyExecutionStore exists with exec dir that has no analysisTools"() {
        given:
        RoddyCleanupJob job = buildJob()
        WorkflowStep workflowStep = buildStep()

        Path execDir = Files.createDirectories(tempDir.resolve('roddyExecutionStore').resolve('exec_2024-01-01'))

        when:
        job.execute(workflowStep)

        then:
        1 * job.fileService.deleteDirectoryRecursively(execDir.resolve('analysisTools'))
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }
}
