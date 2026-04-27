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
import de.dkfz.tbi.otp.dataprocessing.BamImportInstance
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
                BamImportInstance,
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

    private Map createJobForOperation(BamImportInstance.LinkOperation linkOperation, String dirSuffix, String md5sum = null) {
        WorkflowStep ws = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])

        ExternallyProcessedBamFile bamFile = createBamFile(furtherFiles: FURTHER_FILE_NAMES, md5sum: md5sum)
        createBamImportInstance(
                externallyProcessedBamFiles: [bamFile],
                linkOperation: linkOperation,
        )

        Path target = tempDir.resolve("target-${dirSuffix}")
        Files.createDirectories(target)

        Path bamPath = target.resolve(bamFile.fileName)
        Path baiPath = target.resolve("${bamFile.fileName}.bai")

        Path source = tempDir.resolve("source-${dirSuffix}")
        Files.createDirectories(source)

        bamFile.furtherFiles.each {
            Path furtherFilesPath = source.resolve(it)
            if (it.endsWith("directory") || it.endsWith("directory2")) {
                Files.createDirectories(furtherFilesPath)
            } else {
                Files.createFile(furtherFilesPath)
            }
        }

        BamImportValidationJob validationJob = new BamImportValidationJob()
        validationJob.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(ws, BamImportValidationJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
        validationJob.externalAlignmentWorkFileService = Mock(ExternalAlignmentWorkFileService) {
            getDirectoryPath(bamFile) >> target
            getBamFile(bamFile) >> bamPath
            getBaiFile(bamFile) >> baiPath
        }
        validationJob.externalAlignmentSourceFileService = Mock(ExternalAlignmentSourceFileService) {
            getDirectoryPath(bamFile) >> source
        }

        return [workflowStep: ws, job: validationJob, targetDir: target, bamFilePath: bamPath, baiFilePath: baiPath]
    }

    void setup() {
        Map fixture = createJobForOperation(BamImportInstance.LinkOperation.COPY_AND_KEEP, "copy", "d41d8cd98f00b204e9800998ecf8427e")
        workflowStep = fixture.workflowStep
        job = fixture.job
        targetDir = fixture.targetDir
        bamFilePath = fixture.bamFilePath
        baiFilePath = fixture.baiFilePath
    }

    void "test getExpectedFiles"() {
        given:
        List<Path> result = job.getExpectedFiles(workflowStep)

        expect:
        TestCase.assertContainSame(result, [
                bamFilePath,
                baiFilePath,
                targetDir.resolve("${bamFilePath.fileName}.md5sum"),
                targetDir.resolve("${baiFilePath.fileName}.md5sum"),
                targetDir.resolve('test.txt'),
                targetDir.resolve('directory/test1.txt'),
                targetDir.resolve('directory/directory2/test2.txt'),
        ])
    }

    void "test getExpectedFiles, when no md5sum provided, should not include md5sum files"() {
        given:
        Map fixture = createJobForOperation(BamImportInstance.LinkOperation.LINK_SOURCE, "linksource")

        when:
        List<Path> result = fixture.job.getExpectedFiles(fixture.workflowStep)

        then:
        TestCase.assertContainSame(result, [
                fixture.bamFilePath,
                fixture.baiFilePath,
                fixture.targetDir.resolve('test.txt'),
                fixture.targetDir.resolve('directory/test1.txt'),
                fixture.targetDir.resolve('directory/directory2/test2.txt'),
        ])
    }

    void "test getExpectedFiles, when link source operation with md5sum, should include md5sum files"() {
        given:
        Map fixture = createJobForOperation(BamImportInstance.LinkOperation.LINK_SOURCE, "linksource-md5", "d41d8cd98f00b204e9800998ecf8427e")

        when:
        List<Path> result = fixture.job.getExpectedFiles(fixture.workflowStep)

        then:
        TestCase.assertContainSame(result, [
                fixture.bamFilePath,
                fixture.baiFilePath,
                fixture.targetDir.resolve("${fixture.bamFilePath.fileName}.md5sum"),
                fixture.targetDir.resolve("${fixture.baiFilePath.fileName}.md5sum"),
                fixture.targetDir.resolve('test.txt'),
                fixture.targetDir.resolve('directory/test1.txt'),
                fixture.targetDir.resolve('directory/directory2/test2.txt'),
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
