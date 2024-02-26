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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.PanCancerWorkflowDomainFactory
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class PanCancerCleanUpJobSpec extends Specification implements DataTest, PanCancerWorkflowDomainFactory, RoddyPanCancerFactory {

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

    void "getAdditionalPathsToDelete, should return all additional paths that need to be deleted"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        RoddyBamFile bamFile = createRoddyBamFile(RoddyBamFile)
        Path file1 = tempDir.resolve("file1")
        Files.createFile(file1)
        Path file2 = tempDir.resolve("file2")
        Files.createFile(file2)
        Path dir1 = tempDir.resolve("dir1")
        Files.createDirectory(dir1)
        Path dir2 = tempDir.resolve("dir2")
        Files.createDirectory(dir2)

        PanCancerCleanUpJob job = new PanCancerCleanUpJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> bamFile
            0 * _
        }
        job.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            1 * getUnusedResultFiles(_) >> [dir1, file1]
            1 * getOldAdditionalResults(_) >> [dir2, file2]
        }
        job.fileService = Mock(FileService)

        expect:
        TestCase.assertContainSame(job.getAdditionalPathsToDelete(workflowStep), [dir1, file1, dir2, file2])
    }

    void "getWorkFoldersToClear, should return the work folders of all old roddy bam files"() {
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        RoddyBamFile bamFile1 = createRoddyBamFile(RoddyBamFile)
        RoddyBamFile bamFile2 = createRoddyBamFile([workPackage: bamFile1.mergingWorkPackage], RoddyBamFile)
        RoddyBamFile bamFile3 = createRoddyBamFile([workPackage: bamFile1.mergingWorkPackage], RoddyBamFile)
        WorkFolder workFolder = createWorkFolder()

        PanCancerCleanUpJob job = new PanCancerCleanUpJob()
        job.roddyBamFileService = Mock(RoddyBamFileService) {
            1 * getWorkFolder(bamFile2) >> workFolder
            1 * getWorkFolder(bamFile3) >> null
            0 * _
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> bamFile1
            0 * _
        }
        job.fileService = Mock(FileService)

        expect:
        TestCase.assertContainSame(job.getWorkFoldersToClear(workflowStep), [workFolder])
    }
}
