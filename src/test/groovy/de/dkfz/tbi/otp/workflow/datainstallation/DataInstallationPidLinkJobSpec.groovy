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
package de.dkfz.tbi.otp.workflow.datainstallation

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.DataInstallationWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class DataInstallationPidLinkJobSpec extends Specification implements DataTest, DataInstallationWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    void "test getLinkMap"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        Path target1 = Paths.get("target1")
        Path target2 = Paths.get("target2")
        Path link1 = Paths.get("link1")
        Path link2 = Paths.get("link2")

        DataInstallationPidLinkJob job = new DataInstallationPidLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            2 * getFileFinalPathAsPath(_) >>> [target1, target2]
            2 * getFileViewByPidPathAsPath(_) >>> [link1, link2]
        }

        expect:
        [new LinkEntry(target: target1, link: link1), new LinkEntry(target: target2, link: link2)] == job.getLinkMap(workflowStep)
    }

    void "test saveResult"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()

        DataInstallationPidLinkJob job = new DataInstallationPidLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }

        when:
        job.saveResult(workflowStep)

        then:
        seqTrack.sequenceFiles.every { it.fileLinked }
        seqTrack.sequenceFiles.every { it.dateLastChecked }
        seqTrack.dataInstallationState == SeqTrack.DataProcessingState.FINISHED
        seqTrack.fastqcState == SeqTrack.DataProcessingState.NOT_STARTED
    }
}
