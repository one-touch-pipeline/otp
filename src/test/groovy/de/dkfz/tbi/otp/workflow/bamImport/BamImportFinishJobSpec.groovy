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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.ChecksumFileService
import de.dkfz.tbi.otp.ngsdata.IndividualService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*

class BamImportFinishJobSpec extends Specification implements DataTest, BamImportWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                WorkflowStep,
                BamImportInstance,
        ]
    }

    @TempDir
    Path tempDir

    WorkflowStep workflowStep
    ExternallyProcessedBamFile bamFile
    BamImportFinishJob job

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        bamFile = createBamFile()
        job = new BamImportFinishJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportFinishJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
        job.fileSystemService = new TestFileSystemService()
    }

    void "updateDomains should update roddyBamFile with correct values when md5Sum and maximumReadLength are not given"() {
        given:
        TestConfigService testConfigService = new TestConfigService(tempDir)
        WorkflowArtefactService workflowArtefactService = new WorkflowArtefactService()
        bamFile.workflowArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                workflowStep.workflowRun, BamImportWorkflow.OUTPUT_BAM, ArtefactType.BAM, ["Dummy"]
        ))

        job.externallyProcessedBamFileService = new ExternallyProcessedBamFileService([
                abstractBamFileService: new AbstractBamFileService([
                        individualService: new IndividualService([
                                projectService: new ProjectService([
                                        fileSystemService: new TestFileSystemService(),
                                        configService    : testConfigService,
                                ]),
                        ]),
                ]),
                filestoreService: new FilestoreService([
                        fileSystemService: new TestFileSystemService(),
                ]),
        ])
        job.checksumFileService = new ChecksumFileService()
        job.checksumFileService.fileService = new FileService()
        job.checksumFileService.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        job.fileService = Mock(FileService) {
            _ * ensureFileIsReadableAndNotEmpty(_)
        }
        Path testWorkBamFile = CreateFileHelper.createFile(job.externallyProcessedBamFileService.getBamFile(bamFile))
        String path = job.externallyProcessedBamFileService.getBamFile(bamFile)
        Path md5Path = job.fileSystemService.remoteFileSystem.getPath(job.checksumFileService.md5FileName(path))
        CreateFileHelper.createFile(md5Path, "${HelperUtils.randomMd5sum} epmbfName")
        CreateFileHelper.createFile(job.externallyProcessedBamFileService.getBamMaxReadLengthFile(bamFile), "123")

        when:
        job.updateDomains(workflowStep)

        then:
        noExceptionThrown()
        bamFile.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
        bamFile.fileSize == Files.size(testWorkBamFile)
        bamFile.workPackage.bamFileInProjectFolder == bamFile
        bamFile.maximumReadLength == 123
        bamFile.md5sum
    }

    void "updateDomains should update roddyBamFile with correct values when md5Sum and maximumReadLength are given"() {
        given:
        bamFile.md5sum = HelperUtils.randomMd5sum
        bamFile.maximumReadLength = 123
        bamFile.importedFrom = tempDir.resolve("bamFiles")

        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            _ * getBamFile(bamFile, PathOption.REAL_PATH) >> Paths.get(bamFile.importedFrom).resolve(bamFile.bamFileName)
        }

        Path bamFilePath = job.externallyProcessedBamFileService.getBamFile(bamFile, PathOption.REAL_PATH)
        Path testWorkBamFile = CreateFileHelper.createFile(bamFilePath)

        when:
        job.updateDomains(workflowStep)

        then:
        noExceptionThrown()
        bamFile.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
        bamFile.fileSize == Files.size(testWorkBamFile)
        bamFile.workPackage.bamFileInProjectFolder == bamFile
        bamFile.maximumReadLength == 123
        bamFile.md5sum
    }
}
