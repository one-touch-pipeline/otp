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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class BamImportLinkJobSpec extends Specification implements DataTest, BamImportWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                WorkflowStep,
        ]
    }

    void "test getLinkMap"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        ExternallyProcessedBamFile bamFile = createBamFile(furtherFiles: ['test.txt', 'asdf.genes'])

        Path importFolder = Paths.get("/import")
        Path workFolder = Paths.get("/work")

        BamImportLinkJob job = new BamImportLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportLinkJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            getImportFolder(bamFile) >> importFolder
            getImportFolder(bamFile, PathOption.REAL_PATH) >> workFolder
        }

        when:
        List<LinkEntry> result = job.getLinkMap(workflowStep)

        then:
        TestCase.assertContainSame(result, [
                new LinkEntry(link: importFolder.resolve(bamFile.fileName), target: workFolder.resolve(bamFile.fileName)),
                new LinkEntry(link: importFolder.resolve(bamFile.baiFileName), target: workFolder.resolve(bamFile.baiFileName)),
                new LinkEntry(link: importFolder.resolve('test.txt'), target: workFolder.resolve('test.txt')),
                new LinkEntry(link: importFolder.resolve('asdf.genes'), target: workFolder.resolve('asdf.genes')),
        ])
    }
}
