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

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class DataInstallationValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                FastqImportInstance,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    void "test getExpectedFiles"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneDataFile()
        WorkflowStep workflowStep = createWorkflowStep()
        DataInstallationValidationJob job = Spy(DataInstallationValidationJob) {
            _ * getSeqTrack(workflowStep) >> seqTrack
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(_) >> { DataFile dataFile -> dataFile.fileName }
        }
        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }

        when:
        List<Path> result = job.getExpectedFiles(workflowStep)

        then:
        containSame(result*.fileName*.toString(), seqTrack.dataFiles*.fileName)
    }

    void "test getExpectedDirectories"() {
        given:
        DataInstallationValidationJob job = new DataInstallationValidationJob()
        WorkflowStep workflowStep = createWorkflowStep()

        expect:
        [] == job.getExpectedDirectories(workflowStep)
    }

    void "test doFurtherValidation"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()
        seqTrack.linkedExternally = isLinked
        seqTrack.save(flush: true)
        WorkflowStep workflowStep = createWorkflowStep()
        DataInstallationValidationJob job = Spy(DataInstallationValidationJob) {
            _ * getSeqTrack(workflowStep) >> seqTrack
        }
        job.checksumFileService = Mock(ChecksumFileService)

        when:
        job.doFurtherValidation(workflowStep)

        then:
        (isLinked ? 0 : 1) * job.checksumFileService.compareMd5(seqTrack.dataFiles.first()) >> true
        (isLinked ? 0 : 1) * job.checksumFileService.compareMd5(seqTrack.dataFiles.last()) >> true

        where:
        isLinked << [true, false]
    }

    void "test saveResult"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneDataFile()
        WorkflowStep workflowStep = createWorkflowStep()

        DataInstallationValidationJob job = Spy(DataInstallationValidationJob) {
            _ * getSeqTrack(workflowStep) >> seqTrack
        }
        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileFinalPath(_) >> Paths.get("")
        }

        when:
        job.saveResult(workflowStep)

        then:
        seqTrack.dataFiles.every { it.fileSize }
        seqTrack.dataFiles.every { it.dateFileSystem }
        seqTrack.dataFiles.every { it.fileExists }
    }
}
