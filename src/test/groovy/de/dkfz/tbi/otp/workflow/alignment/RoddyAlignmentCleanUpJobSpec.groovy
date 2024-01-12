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
package de.dkfz.tbi.otp.workflow.alignment

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.PanCancerWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class RoddyAlignmentCleanUpJobSpec  extends Specification implements DataTest, PanCancerWorkflowDomainFactory, RoddyRnaFactory {
    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                RnaRoddyBamFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FastqImportInstance,
                FastqFile,
        ]
    }

    class TestAbstractRoddyAlignmentCleanUpJob extends AbstractRoddyAlignmentCleanUpJob { }

    void "test getFilesToDelete"() {
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        AbstractRoddyAlignmentCleanUpJob job = new TestAbstractRoddyAlignmentCleanUpJob()

        expect:
        [] == job.getFilesToDelete(workflowStep)
    }

    void "test getDirectoriesToDelete"() {
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        RoddyBamFile bamFile1 = createRoddyBamFile(RoddyBamFile)
        RoddyBamFile bamFile2 = createRoddyBamFile([workPackage: bamFile1.mergingWorkPackage, config: bamFile1.config], RoddyBamFile)

        AbstractRoddyAlignmentCleanUpJob job = new TestAbstractRoddyAlignmentCleanUpJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> bamFile1
            0 * _
        }

        Path dir1 = tempDir.resolve("dir1")
        Files.createDirectory(dir1)
        Path dir2 = tempDir.resolve("dir2")
        Files.createDirectory(dir2)
        job.roddyBamFileService = Mock(RoddyBamFileService) {
            1 * getWorkDirectory(bamFile2) >> dir2
        }

        expect:
        [dir2] == job.getDirectoriesToDelete(workflowStep)
    }
}
