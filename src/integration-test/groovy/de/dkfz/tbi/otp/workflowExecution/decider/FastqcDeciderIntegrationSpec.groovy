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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.FastqcWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

@Rollback
@Integration
class FastqcDeciderIntegrationSpec extends Specification implements FastqcWorkflowDomainFactory {

    void "test decide"() {
        given:
        WorkflowVersion workflowVersion = createBashFastqcWorkflowVersion()
        Workflow workflow = workflowVersion.workflow

        WorkflowArtefact wa1 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ, producedBy: createWorkflowRun([workflowVersion: workflowVersion]))
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile(workflowArtefact: wa1)
        WorkflowArtefact wa12 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ, producedBy: createWorkflowRun([workflowVersion: workflowVersion])) // run finished already
        createWorkflowRunInputArtefact(workflowArtefact: wa12, workflowRun: createWorkflowRun([workflowVersion: workflowVersion]))
        WorkflowArtefact wa2 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ) // without artefact
        WorkflowArtefact wa3 = createWorkflowArtefact(artefactType: ArtefactType.FASTQC) // wrong type
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)

        FastqcDecider decider = new FastqcDecider()
        decider.seqTrackService = Mock(SeqTrackService) {
            1 * getSequenceFilesForSeqTrack(seqTrack) >> dataFiles
            0 * _
        }
        decider.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(_ as FastqcProcessedFile) >> Paths.get("/output-dir-fastqc")
            0 * _
        }
        decider.configFragmentService = Mock(ConfigFragmentService) {
            1 * getSortedFragments(_) >> [new ExternalWorkflowConfigFragment(name: "xyz", configValues: '{"WORKFLOWS":{"resource":"1"}}')]
            0 * _
        }
        decider.workflowService = Mock(WorkflowService) {
            1 * getExactlyOneWorkflow(BashFastQcWorkflow.WORKFLOW) >> workflow
            0 * _
        }
        decider.fastQcProcessedFileService = Mock(FastQcProcessedFileService) {
            1 * buildWorkingPath(workflowVersion) >> "buildPath"
            0 * _
        }
        decider.workflowRunService = new WorkflowRunService()
        decider.workflowRunService.configFragmentService = new ConfigFragmentService()
        decider.workflowArtefactService = new WorkflowArtefactService()

        when:
        DeciderResult result = decider.decide([wa1, wa2, wa3])

        then:
        result.warnings.empty
        result.newArtefacts.size() == 2
        result.newArtefacts.every {
            assert it.artefactType == ArtefactType.FASTQC
            true
        }
        WorkflowRunInputArtefact inputArtefact = CollectionUtils.exactlyOneElement(WorkflowRunInputArtefact.findAllByWorkflowArtefact(wa1))
        inputArtefact.workflowRun
        inputArtefact.workflowRun.workflow == workflow
        inputArtefact.workflowRun.combinedConfig == '{"WORKFLOWS":{"resource":"1"}}'
        inputArtefact.workflowRun.workDirectory == "/output-dir-fastqc"
        TestCase.assertContainSame(result.newArtefacts*.artefact*.get()*.dataFile, dataFiles)
    }
}
