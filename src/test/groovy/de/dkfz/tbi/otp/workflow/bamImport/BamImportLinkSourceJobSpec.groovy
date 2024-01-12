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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

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
    void "test getLinkMap should return list with entries when link source is true"() {
        given:
        createImportInstance(externallyProcessedBamFiles: [bamFile], linkOperation: BamImportInstance.LinkOperation.LINK_SOURCE)
        Path targetBaseDirFilePath = Paths.get("/source")
        Path sourceBamFilePath = targetBaseDirFilePath.resolve(Paths.get(bamFile.bamFileName))
        Path sourceBaiFilePath = targetBaseDirFilePath.resolve(Paths.get(bamFile.baiFileName))
        Path realTargetBamImportFolder = Paths.get("/target")
        Path targetBamFilePath = realTargetBamImportFolder.resolve(Paths.get(bamFile.bamFileName))
        Path targetBaiFilePath = realTargetBamImportFolder.resolve(Paths.get(bamFile.baiFileName))
        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            1 * getImportFolder(bamFile, PathOption.REAL_PATH) >> targetBaseDirFilePath
            1 * getSourceBaseDirFilePath(bamFile) >> realTargetBamImportFolder
        }
        expect:
        job.getLinkMap(workflowStep) == [
                new LinkEntry(link: sourceBamFilePath, target: targetBamFilePath),
                new LinkEntry(link: sourceBaiFilePath, target: targetBaiFilePath),
                new LinkEntry(link: targetBaseDirFilePath.resolve("file1"), target: realTargetBamImportFolder.resolve("file1")),
                new LinkEntry(link: targetBaseDirFilePath.resolve("file2"), target: realTargetBamImportFolder.resolve("file2")),
        ]
    }

    void "test getLinkMap should return empty list when link source is false"() {
        given:
        createImportInstance(externallyProcessedBamFiles: [bamFile])
        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService)

        expect:
        job.getLinkMap(workflowStep) == []
    }
}
