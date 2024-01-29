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

import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class BamImportConditionalFailJobSpec extends Specification implements DataTest, BamImportWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                WorkflowStep,
        ]
    }

    WorkflowStep workflowStep

    BamImportConditionalFailJob job

    @TempDir
    Path tempDir

    Path sourceDir
    Path bamFilePath
    Path baiFilePath

    static final List<String> FURTHER_FILE_NAMES = [
            'test1.txt',
            'directory',
            'directory/test2.txt',
            'directory/directory2',
            'directory/directory2/test3.txt',
    ].asImmutable()

    static final List<String> FURTHER_FILES_EXIST_IN_SOURCE_DIR = [
            'test1.txt',
            'directory',
            'directory/directory2/test3.txt',
    ].asImmutable()

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        ExternallyProcessedBamFile bamFile = createBamFile(furtherFiles: FURTHER_FILE_NAMES)

        sourceDir = tempDir.resolve("source")
        Files.createDirectory(sourceDir)

        bamFilePath = sourceDir.resolve(bamFile.fileName)
        CreateFileHelper.createFile(bamFilePath, "bam file")

        baiFilePath = sourceDir.resolve("${bamFile.fileName}.bai")
        CreateFileHelper.createFile(baiFilePath, "bai file")

        job = new BamImportConditionalFailJob()
        job.fileService = Mock(FileService) {
            _ * fileIsReadable(_) >> { Path path ->
                return Files.isReadable(path)
            }
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportConditionalFailJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            getSourceBamFilePath(bamFile) >> bamFilePath
            getSourceBaiFilePath(bamFile) >> baiFilePath
            getSourceBaseDirFilePath(bamFile) >> sourceDir
        }
    }

    void "test check method with all files present"() {
        given:
        FURTHER_FILE_NAMES.collect {
            Path furtherFilesSourcePath = sourceDir.resolve(it)
            if (it.endsWith("directory") || it.endsWith("directory2")) {
                Files.createDirectory(furtherFilesSourcePath)
            } else {
                CreateFileHelper.createFile(furtherFilesSourcePath, "dummy")
            }
        }

        when:
        job.check(workflowStep)

        then:
        noExceptionThrown()
    }

    void "test check method with some further files missing"() {
        given:

        FURTHER_FILES_EXIST_IN_SOURCE_DIR.collect {
            Path furtherFilesSourcePath = sourceDir.resolve(it)
            if (it.endsWith("directory") || it.endsWith("directory2")) {
                Files.createDirectories(furtherFilesSourcePath)
            } else {
                CreateFileHelper.createFile(furtherFilesSourcePath, "dummy")
            }
            return furtherFilesSourcePath
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are missing")
        e.message.contains("directory/test2.txt")
    }
}
