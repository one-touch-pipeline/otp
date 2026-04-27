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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentSourceFileService
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentWorkFileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class BamImportLinkSourceJobSpec extends Specification implements DataTest, BamImportWorkflowDomainFactory {
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
    BamImportLinkSourceJob job

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        bamFile = createBamFile(furtherFiles: ["file1", "file2"])
        job = new BamImportLinkSourceJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportLinkSourceJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
    }

    private Map setupLinkSourceMocks(String workDirName) {
        Path workDir = tempDir.resolve(workDirName)
        Files.createDirectories(workDir)
        Path sourceDir = Paths.get("/source")
        Path md5sumPath = workDir.resolve("${bamFile.bamFileName}.md5sum")

        job.externalAlignmentWorkFileService = Mock(ExternalAlignmentWorkFileService) {
            _ * getDirectoryPath(bamFile) >> workDir
            _ * getMd5SumPath(bamFile) >> md5sumPath
            _ * getMd5SumPathBai(bamFile) >> workDir.resolve("${bamFile.baiFileName}.md5sum")
        }
        job.externalAlignmentSourceFileService = Mock(ExternalAlignmentSourceFileService) {
            1 * getDirectoryPath(bamFile) >> sourceDir
        }
        return [workDir: workDir, sourceDir: sourceDir, md5sumPath: md5sumPath]
    }

    void "test getLinkMap, when link source without md5sum, should return links and neither write BAM md5sum nor compute BAI md5sum"() {
        given:
        createBamImportInstance(externallyProcessedBamFiles: [bamFile], linkOperation: BamImportInstance.LinkOperation.LINK_SOURCE)
        Map dirs = setupLinkSourceMocks("work")
        job.remoteShellHelper = Mock(RemoteShellHelper) {
            0 * executeCommandReturnProcessOutput(_)
        }

        when:
        List<LinkEntry> result = job.getLinkMap(workflowStep)

        then:
        result == [
                new LinkEntry(link: dirs.workDir.resolve(bamFile.bamFileName), target: dirs.sourceDir.resolve(bamFile.bamFileName)),
                new LinkEntry(link: dirs.workDir.resolve(bamFile.baiFileName), target: dirs.sourceDir.resolve(bamFile.baiFileName)),
                new LinkEntry(link: dirs.workDir.resolve("file1"), target: dirs.sourceDir.resolve("file1")),
                new LinkEntry(link: dirs.workDir.resolve("file2"), target: dirs.sourceDir.resolve("file2")),
        ]
        !Files.exists(dirs.md5sumPath)
    }

    void "test getLinkMap, when link source with md5sum, should write md5sum file and compute BAI md5sum"() {
        given:
        String md5sumValue = "d41d8cd98f00b204e9800998ecf8427e"
        bamFile.md5sum = md5sumValue
        bamFile.save(flush: true)
        createBamImportInstance(externallyProcessedBamFiles: [bamFile], linkOperation: BamImportInstance.LinkOperation.LINK_SOURCE)
        Map dirs = setupLinkSourceMocks("work-md5")
        job.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_) >> new ProcessOutput(stdout: "", stderr: "", exitCode: 0)
        }

        when:
        List<LinkEntry> result = job.getLinkMap(workflowStep)

        then:
        result == [
                new LinkEntry(link: dirs.workDir.resolve(bamFile.bamFileName), target: dirs.sourceDir.resolve(bamFile.bamFileName)),
                new LinkEntry(link: dirs.workDir.resolve(bamFile.baiFileName), target: dirs.sourceDir.resolve(bamFile.baiFileName)),
                new LinkEntry(link: dirs.workDir.resolve("file1"), target: dirs.sourceDir.resolve("file1")),
                new LinkEntry(link: dirs.workDir.resolve("file2"), target: dirs.sourceDir.resolve("file2")),
        ]
        Files.exists(dirs.md5sumPath)
        Files.readString(dirs.md5sumPath) == md5sumValue
    }

    void "test getLinkMap should return empty list when link source is false"() {
        given:
        createBamImportInstance(externallyProcessedBamFiles: [bamFile])

        expect:
        job.getLinkMap(workflowStep) == []
    }
}
