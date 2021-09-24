/*
 * Copyright 2011-2021 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.fastqc.FastqcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

@Rollback
@Integration
class FastqcDeciderIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    void "test decide"() {
        given:
        Workflow workflow = createWorkflow(name: FastqcWorkflow.WORKFLOW)

        WorkflowArtefact wa1 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ, producedBy: createWorkflowRun())
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile(workflowArtefact: wa1)
        WorkflowArtefact wa12 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ, producedBy: createWorkflowRun()) // already run
        createWorkflowRunInputArtefact(workflowArtefact: wa12, workflowRun: createWorkflowRun(workflow: workflow))
        WorkflowArtefact wa2 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ) // without artefact
        WorkflowArtefact wa3 = createWorkflowArtefact(artefactType: ArtefactType.FASTQC) // wrong type
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)

        FastqcDecider decider = new FastqcDecider()
        decider.seqTrackService = Mock(SeqTrackService) {
            1 * getSequenceFilesForSeqTrack(seqTrack) >> dataFiles
        }
        decider.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(seqTrack) >> Paths.get("/output-dir-fastqc")
        }
        decider.configFragmentService = Mock(ConfigFragmentService) {
            1 * getSortedFragments(_) >> [new ExternalWorkflowConfigFragment(name: "xyz", configValues: '{"WORKFLOWS":{"resource":"1"}}')]
        }
        decider.workflowRunService = new WorkflowRunService()
        decider.workflowRunService.configFragmentService = new ConfigFragmentService()
        decider.workflowArtefactService = new WorkflowArtefactService()

        when:
        Collection<WorkflowArtefact> result = decider.decide([wa1, wa2, wa3])

        then:
        result.size() == 2
        result.every {
            assert it.artefactType == ArtefactType.FASTQC
            true
        }
        WorkflowRunInputArtefact inputArtefact = CollectionUtils.exactlyOneElement(WorkflowRunInputArtefact.findAllByWorkflowArtefact(wa1))
        inputArtefact.workflowRun
        inputArtefact.workflowRun.workflow == workflow
        inputArtefact.workflowRun.combinedConfig == '{"WORKFLOWS":{"resource":"1"}}'
        inputArtefact.workflowRun.workDirectory == "/output-dir-fastqc"
        TestCase.assertContainSame(result*.artefact*.get()*.dataFile , dataFiles)
    }
}
