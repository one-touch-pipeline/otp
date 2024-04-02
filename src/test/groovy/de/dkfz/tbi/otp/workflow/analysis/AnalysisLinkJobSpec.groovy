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
package de.dkfz.tbi.otp.workflow.analysis

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.workflowSystem.AceseqWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class AnalysisLinkJobSpec extends Specification implements DataTest, AceseqWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                RoddyBamFile,
        ]
    }

    WorkflowStep workflowStep
    BamFilePairAnalysis analysisFile
    AnalysisLinkJob job

    void "getLinkMap should return link for work directory to link directory "() {
        given:
        job = new AnalysisLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService)
        job.analysisWorkFileServiceFactoryService = Mock(AnalysisWorkFileServiceFactoryService)
        job.analysisLinkFileServiceFactoryService = Mock(AnalysisLinkFileServiceFactoryService)
        AbstractAnalysisWorkFileService workFileService = Mock(AbstractAnalysisWorkFileService)
        AbstractAnalysisLinkFileService linkFileService = Mock(AbstractAnalysisLinkFileService)

        analysisFile = Mock(BamFilePairAnalysis)

        Path workDir = Paths.get('workDir')
        Path linkDir = Paths.get('linkDir')

        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateAceseqWorkflow()])])

        when:
        List<LinkEntry> links = job.getLinkMap(workflowStep)

        then:
        TestCase.assertContainSame(links, [new LinkEntry([target: workDir, link: linkDir])])

        and:
        1 * job.analysisWorkFileServiceFactoryService.getService(analysisFile) >> workFileService
        1 * job.analysisLinkFileServiceFactoryService.getService(analysisFile) >> linkFileService
        1 * linkFileService.getDirectoryPath(analysisFile) >> linkDir
        1 * workFileService.getDirectoryPath(analysisFile) >> workDir
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, AbstractAnalysisWorkflow.ANALYSIS_OUTPUT) >> analysisFile
        0 * _
    }
}
