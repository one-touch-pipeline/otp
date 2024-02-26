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
package de.dkfz.tbi.otp.workflow.alignment.rna

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.RnaAlignmentWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class RnaAlignmentCleanUpJobSpec extends Specification implements DataTest, RnaAlignmentWorkflowDomainFactory, RoddyRnaFactory {
    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                RnaRoddyBamFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
        ]
    }

    void "getAdditionalPathsToDelete, should return the correct paths"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateRnaAlignmentWorkflow(),
                ]),
        ])
        RnaRoddyBamFile bamFile = createRoddyBamFile(RnaRoddyBamFile)

        RnaAlignmentCleanUpJob job = new RnaAlignmentCleanUpJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, RnaAlignmentWorkflow.OUTPUT_BAM) >> bamFile
            0 * _
        }
        job.fileService = Mock(FileService) {
            fileIsReadable(_) >> true
        }

        Path dir1 = tempDir.resolve("dir1")
        Files.createDirectory(dir1)
        Path file1 = dir1.resolve("file1")
        Files.createFile(file1)
        Path link1 = dir1.resolve("link")
        Files.createSymbolicLink(link1, file1)
        job.roddyBamFileService = Mock(RoddyBamFileService) {
            1 * getBaseDirectory(bamFile) >> dir1
        }

        expect:
        job.getAdditionalPathsToDelete(workflowStep) == [link1]
    }
}
