/*
 * Copyright 2011-2021 The OTP authors
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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.Md5SumService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class PanCancerFinishJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqImportInstance,
                FileType,
                MergingWorkPackage,
                ProcessingOption,
                RoddyBamFile,
                WorkflowStep,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    void "test updateDomain method should update roddyBamFile"() {
        given:
        TestConfigService testConfigService = new TestConfigService(temporaryFolder.newFolder())
        WorkflowStep workflowStep = createWorkflowStep()
        MergingWorkPackage workPackage = createMergingWorkPackage(bamFileInProjectFolder: null)
        RoddyBamFile roddyBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                workPackage: workPackage,
        ])

        Path testWorkBamFile = CreateFileHelper.createFile(roddyBamFile.workBamFile.toPath())
        String md5Sum = "098f6bcd4621d373cade4e832627b4f6"

        PanCancerFinishJob job = Spy(PanCancerFinishJob) {
            1 * getRoddyBamFile(workflowStep) >> roddyBamFile
        }

        job.md5SumService = Mock(Md5SumService) {
            1 * extractMd5Sum(_) >> md5Sum
        }

        job.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            1 * updateSamplePairStatusToNeedProcessing(roddyBamFile)
        }

        job.roddyBamFileService = new RoddyBamFileService([
                abstractMergedBamFileService: new AbstractMergedBamFileService([
                        individualService: new IndividualService([
                                projectService: new ProjectService([
                                        fileSystemService: new TestFileSystemService(),
                                        configService: testConfigService,
                                ]),
                        ]),
                ]),
        ])

        job.fileSystemService = new TestFileSystemService()
        job.fileService = new FileService()

        when:
        job.updateDomains(workflowStep)

        then:
        RoddyBamFile bamFileAfterUpdate = RoddyBamFile.findAll().first()
        bamFileAfterUpdate.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        bamFileAfterUpdate.workPackage.bamFileInProjectFolder == bamFileAfterUpdate
        bamFileAfterUpdate.md5sum == md5Sum
        bamFileAfterUpdate.fileSize == Files.size(testWorkBamFile)
        bamFileAfterUpdate.fileSize == testWorkBamFile.toFile().length()
    }
}
