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

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellMappingFileService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.DataInstallationWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataAllWellFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class DataInstallationSingleCellLinkJobSpec extends Specification implements DataTest, DataInstallationWorkflowDomainFactory {

    WorkflowStep workflowStep
    SeqTrack seqTrack

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

    void setupData(boolean isSingleCell) {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])
        seqTrack = createSeqTrackWithTwoFastqFile()
        if (isSingleCell) {
            seqTrack.seqType = DomainFactory.createCellRangerAlignableSeqTypes().first()
            seqTrack.singleCellWellLabel = "label"
            seqTrack.save(flush: true)
        }
    }

    void "test getLinkMap"() {
        given:
        setupData(isSingleCell)

        Path target1 = Paths.get("target1")
        Path target2 = Paths.get("target2")
        Path link1 = Paths.get("link1")
        Path link2 = Paths.get("link2")

        DataInstallationSingleCellLinkJob job = new DataInstallationSingleCellLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }
        job.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            (isSingleCell ? 2 : 0) * getFilePath(_) >>> [target1, target2]
        }
        job.rawSequenceDataAllWellFileService = Mock(RawSequenceDataAllWellFileService) {
            (isSingleCell ? 2 : 0) * getFilePath(_) >>> [link1, link2]
        }

        expect:
        (isSingleCell ? [new LinkEntry(target: target1, link: link1), new LinkEntry(target: target2, link: link2)] : []) == job.getLinkMap(workflowStep)

        where:
        isSingleCell << [true, false]
    }

    void "test doFurtherLinking"() {
        given:
        setupData(isSingleCell)

        DataInstallationSingleCellLinkJob job = new DataInstallationSingleCellLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }
        job.singleCellMappingFileService = Mock(SingleCellMappingFileService)
        job.logService = Mock(LogService)

        when:
        job.doFurtherWork(workflowStep)

        then:
        (isSingleCell ? 2 : 0) * job.singleCellMappingFileService.addMappingFileEntryIfMissing(_)

        where:
        isSingleCell << [true, false]
    }
}
