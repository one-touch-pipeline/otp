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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentSourceFileService
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentWorkFileService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class BamImportValidationJobSpec extends Specification implements DataTest, BamImportWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                WorkflowStep,
        ]
    }

    WorkflowStep workflowStep

    BamImportValidationJob job

    @TempDir
    Path tempDir

    Path targetDir

    Path bamFilePath
    Path baiFilePath

    static final List<String> FURTHER_FILE_NAMES = [
            'test.txt',
            'directory',
            'directory/test1.txt',
            'directory/directory2',
            'directory/directory2/test2.txt',
    ]

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])

        ExternallyProcessedBamFile bamFile = createBamFile(furtherFiles: FURTHER_FILE_NAMES)

        targetDir = tempDir.resolve("target")
        Files.createDirectories(targetDir)

        bamFilePath = targetDir.resolve(bamFile.fileName)
        baiFilePath = targetDir.resolve("${bamFile.fileName}.bai")

        Path sourceDir = tempDir.resolve("source")
        Files.createDirectories(sourceDir)

        bamFile.furtherFiles.each {
            Path furtherFilesPath = sourceDir.resolve(it)
            if (it.endsWith("directory") || it.endsWith("directory2")) {
                Files.createDirectories(furtherFilesPath)
            } else {
                Files.createFile(furtherFilesPath)
            }
        }

        job = new BamImportValidationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportValidationJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
        job.externalAlignmentWorkFileService = Mock(ExternalAlignmentWorkFileService) {
            getDirectoryPath(bamFile) >> targetDir
            getBamFile(bamFile) >> bamFilePath
            getBaiFile(bamFile) >> baiFilePath
        }
        job.externalAlignmentSourceFileService = Mock(ExternalAlignmentSourceFileService) {
            getDirectoryPath(bamFile) >> sourceDir
        }
    }

    void "test getExpectedFiles"() {
        given:
        List<Path> result = job.getExpectedFiles(workflowStep)

        expect:
        TestCase.assertContainSame(result, [
                bamFilePath,
                baiFilePath,
                targetDir.resolve('test.txt'),
                targetDir.resolve('directory/test1.txt'),
                targetDir.resolve('directory/directory2/test2.txt'),
        ])
    }

    void "test getExpectedFolders"() {
        given:
        List<Path> result = job.getExpectedDirectories(workflowStep)

        expect:
        TestCase.assertContainSame(result, [
                targetDir.resolve('directory'),
                targetDir.resolve('directory/directory2'),
        ])
    }
}
