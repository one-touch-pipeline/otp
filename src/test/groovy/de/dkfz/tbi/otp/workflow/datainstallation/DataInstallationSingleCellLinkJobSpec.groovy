/*
 * Copyright 2011-2020 The OTP authors
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
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

class DataInstallationSingleCellLinkJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                FastqImportInstance,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    WorkflowStep workflowStep
    SeqTrack seqTrack

    void setupData(boolean isSingleCell) {
        workflowStep = createWorkflowStep()
        seqTrack = createSeqTrackWithTwoDataFile()
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

        DataInstallationSingleCellLinkJob job = Spy(DataInstallationSingleCellLinkJob) {
            1 * getSeqTrack(workflowStep) >> seqTrack
        }
        job.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem(_) >> FileSystems.default
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            (isSingleCell ? 2 : 0) * getFileFinalPathAsPath(_, _) >>> [target1, target2]
            (isSingleCell ? 2 : 0) * getWellAllFileViewByPidPathAsPath(_, _) >>> [link1, link2]
        }

        expect:
        (isSingleCell ? [new LinkEntry(target: target1, link: link1), new LinkEntry(target: target2, link: link2)] : []) == job.getLinkMap(workflowStep)

        where:
        isSingleCell << [true, false]
    }

    void "test doFurtherLinking"() {
        given:
        setupData(isSingleCell)

        DataInstallationSingleCellLinkJob job = Spy(DataInstallationSingleCellLinkJob) {
            1 * getSeqTrack(workflowStep) >> seqTrack
        }
        job.singleCellMappingFileService = Mock(SingleCellMappingFileService)

        when:
        job.doFurtherWork(workflowStep)

        then:
        (isSingleCell ? 2 : 0) * job.singleCellMappingFileService.addMappingFileEntryIfMissing(_)

        where:
        isSingleCell << [true, false]
    }
}
