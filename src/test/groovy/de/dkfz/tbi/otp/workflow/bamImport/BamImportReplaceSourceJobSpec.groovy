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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

class BamImportReplaceSourceJobSpec extends Specification implements DataTest, BamImportWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                BamImportInstance,
                WorkflowStep,
        ]
    }

    @TempDir
    Path tempDir

    WorkflowStep workflowStep
    ExternallyProcessedBamFile bamFile
    BamImportReplaceSourceJob job

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        bamFile = createBamFile(furtherFiles: ["file1", "file2"])
        job = new BamImportReplaceSourceJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportReplaceSourceJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
    }

    void "test BamImportReplaceSourceJob.getLinkMap when replaceSourceWithLink is true"() {
        given:
        createImportInstance(externallyProcessedBamFiles: [bamFile], linkOperation: BamImportInstance.LinkOperation.COPY_AND_LINK)
        Path sourceBaseDirFilePath = Paths.get("/source")
        Path sourceBamFilePath = sourceBaseDirFilePath.resolve(Paths.get("bamFile.bam"))
        Path sourceBaiFilePath = sourceBaseDirFilePath.resolve(Paths.get("bamFile.bai"))
        Path targetBaseDirFilePath = Paths.get("/target")
        Path targetBamFilePath = targetBaseDirFilePath.resolve(Paths.get("bamFile.bam"))
        Path targetBaiFilePath = targetBaseDirFilePath.resolve(Paths.get("bamFile.bai"))

        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            1 * getSourceBamFilePath(bamFile) >> sourceBamFilePath
            1 * getSourceBaiFilePath(bamFile) >> sourceBaiFilePath
            1 * getSourceBaseDirFilePath(bamFile) >> sourceBaseDirFilePath
            1 * getBamFile(bamFile) >> targetBamFilePath
            1 * getBaiFile(bamFile) >> targetBaiFilePath
            1 * getImportFolder(bamFile) >> targetBaseDirFilePath
        }

        expect:
        job.getLinkMap(workflowStep) == [
                new LinkEntry(link: sourceBamFilePath, target: targetBamFilePath),
                new LinkEntry(link: sourceBaiFilePath, target: targetBaiFilePath),
                new LinkEntry(link: sourceBaseDirFilePath.resolve("file1"), target: targetBaseDirFilePath.resolve("file1")),
                new LinkEntry(link: sourceBaseDirFilePath.resolve("file2"), target: targetBaseDirFilePath.resolve("file2")),
        ]
    }

    void "test getLinkMap, when furtherFiles contains directory, delete it"() {
        given:
        createImportInstance(externallyProcessedBamFiles: [bamFile], linkOperation: BamImportInstance.LinkOperation.COPY_AND_LINK)
        Path sourceBaseDirFilePath = tempDir
        Path targetBaseDirFilePath = Paths.get("/target")

        Path dir = sourceBaseDirFilePath.resolve("dir")
        Files.createDirectory(dir)
        bamFile.furtherFiles = [dir.fileName.toString()]
        bamFile.save(flush: true)

        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            1 * getSourceBaseDirFilePath(bamFile) >> sourceBaseDirFilePath
            1 * getImportFolder(bamFile) >> targetBaseDirFilePath
        }
        job.fileService = Mock(FileService)

        when:
        job.getLinkMap(workflowStep)

        then:
        1 * job.fileService.deleteDirectoryRecursively(dir)
    }

    void "test BamImportReplaceSourceJob.getLinkMap when replaceSourceWithLink is false"() {
        given:
        createImportInstance(externallyProcessedBamFiles: [bamFile])

        expect:
        job.getLinkMap(workflowStep) == []
    }
}
