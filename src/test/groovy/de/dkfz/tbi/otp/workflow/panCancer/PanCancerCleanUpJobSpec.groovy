/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class PanCancerCleanUpJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, RoddyPancanFactory {

    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                FileType,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    void "test getFilesToDelete"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        RoddyBamFile bamFile = createRoddyBamFile(RoddyBamFile)
        Path file1 = tempDir.resolve("file1")
        Files.createFile(file1)
        Path file2 = tempDir.resolve("file2")
        Files.createFile(file2)
        Path dir1 = tempDir.resolve("dir1")
        Files.createDirectory(dir1)
        Path dir2 = tempDir.resolve("dir2")
        Files.createDirectory(dir2)
        PanCancerCleanUpJob job = Spy(PanCancerCleanUpJob) {
            getRoddyBamFile(workflowStep) >> bamFile
        }
        job.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            1 * getFilesToCleanup(_, _) >> [dir1, file1]
            1 * getOldResultsToCleanup(_, _) >> [dir2, file2]
        }
        job.fileService = Mock(FileService)

        expect:
        [file1, file2] == job.getFilesToDelete(workflowStep)
    }

    void "test getDirectoriesToDelete"() {
        WorkflowStep workflowStep = createWorkflowStep()
        RoddyBamFile bamFile = createRoddyBamFile(RoddyBamFile)
        Path file1 = tempDir.resolve("file1")
        Files.createFile(file1)
        Path file2 = tempDir.resolve("file2")
        Files.createFile(file2)
        Path dir1 = tempDir.resolve("dir1")
        Files.createDirectory(dir1)
        Path dir2 = tempDir.resolve("dir2")
        Files.createDirectory(dir2)
        PanCancerCleanUpJob job = Spy(PanCancerCleanUpJob) {
            getRoddyBamFile(workflowStep) >> bamFile
        }
        job.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            1 * getFilesToCleanup(_, _) >> [dir1, file1]
            1 * getOldResultsToCleanup(_, _) >> [dir2, file2]
        }
        job.fileService = Mock(FileService)

        expect:
        [dir1, dir2] == job.getDirectoriesToDelete(workflowStep)
    }
}
