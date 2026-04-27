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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.BamImportInstance
import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentWorkFileService
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
                BamImportInstance,
                ExternallyProcessedBamFile,
                WorkflowStep,
        ]
    }

    void "test getLinkMap, #testCase"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        ExternallyProcessedBamFile bamFile = createBamFile(
                furtherFiles: ['test.txt', 'asdf.genes'],
                md5sum: md5sum,
        )
        createBamImportInstance(
                externallyProcessedBamFiles: [bamFile],
                linkOperation: linkOperation,
        )

        Path importFolder = Paths.get("/import")
        Path workFolder = Paths.get("/work")

        BamImportLinkJob job = new BamImportLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, BamImportLinkJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
            0 * _
        }
        job.externalAlignmentLinkFileService = Mock(ExternalAlignmentLinkFileService) {
            getDirectoryPath(bamFile) >> importFolder
        }
        job.externalAlignmentWorkFileService = Mock(ExternalAlignmentWorkFileService) {
            getDirectoryPath(bamFile) >> workFolder
        }

        when:
        List<LinkEntry> result = job.getLinkMap(workflowStep)

        then:
        List<LinkEntry> expected = [
                new LinkEntry(link: importFolder.resolve(bamFile.fileName), target: workFolder.resolve(bamFile.fileName)),
                new LinkEntry(link: importFolder.resolve(bamFile.baiFileName), target: workFolder.resolve(bamFile.baiFileName)),
                new LinkEntry(link: importFolder.resolve('test.txt'), target: workFolder.resolve('test.txt')),
                new LinkEntry(link: importFolder.resolve('asdf.genes'), target: workFolder.resolve('asdf.genes')),
        ]
        if (expectMd5sum) {
            expected.add(new LinkEntry(link: importFolder.resolve("${bamFile.fileName}.md5sum"), target: workFolder.resolve("${bamFile.fileName}.md5sum")))
            expected.add(new LinkEntry(link: importFolder.resolve("${bamFile.baiFileName}.md5sum"), target: workFolder.resolve("${bamFile.baiFileName}.md5sum")))
        }
        TestCase.assertContainSame(result, expected)

        where:
        testCase                                     | linkOperation                                 | md5sum                             || expectMd5sum
        "copy operation includes md5sum links"       | BamImportInstance.LinkOperation.COPY_AND_KEEP | null                               || true
        "link source without md5sum excludes md5sum" | BamImportInstance.LinkOperation.LINK_SOURCE   | null                               || false
        "link source with md5sum includes md5sum"    | BamImportInstance.LinkOperation.LINK_SOURCE   | 'd41d8cd98f00b204e9800998ecf8427e' || true
    }
}
