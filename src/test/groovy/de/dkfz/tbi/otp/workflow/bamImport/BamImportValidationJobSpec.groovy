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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

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
    ExternallyProcessedBamFile bamFile
    BamImportValidationJob job

    void "test getExpectedFiles"() {
        given:
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateBamImportWorkflowWorkflow(),
                ]),
        ])
        bamFile = createBamFile(furtherFiles: ['test.txt', 'asdf.genes'])

        Path bamFilePath = Paths.get("/bamFile")
        Path baiFilePath = Paths.get("/baiFile")
        Path furtherFiles1 = Paths.get("/furtherFiles1")
        Path furtherFiles2 = Paths.get("/furtherFiles2")

        job = new BamImportValidationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            getOutputArtefact(workflowStep, BamImportValidationJob.de_dkfz_tbi_otp_workflow_bamImport_BamImportShared__OUTPUT_ROLE) >> bamFile
        }
        job.externallyProcessedBamFileService = Mock(ExternallyProcessedBamFileService) {
            getBamFile(bamFile) >> bamFilePath
            getBaiFile(bamFile) >> baiFilePath
            getFurtherFiles(bamFile) >> [furtherFiles1, furtherFiles2]
        }

        when:
        List<Path> result = job.getExpectedFiles(workflowStep)

        then:
        TestCase.assertContainSame(result, [
                bamFilePath,
                baiFilePath,
                furtherFiles1,
                furtherFiles2,

        ])
    }
}
