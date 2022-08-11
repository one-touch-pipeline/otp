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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystems
import java.nio.file.Path

class PanCancerConditionalFailJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                WorkflowStep,
        ]
    }

    @Unroll
    void "test check #name, succeeds"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [
                createSeqTrackWithTwoDataFile([:], [mateNumber: mateFile1, fileName: file1], [mateNumber: mateFile2, fileName: file2]),
                createSeqTrackWithTwoDataFile([:], [fileName: "SecondSeqTrack_123_R1.gz"], [fileName: "SecondSeqTrack_123_R2.gz"]),
        ]

        PanCancerConditionalFailJob job = Spy(PanCancerConditionalFailJob) {
            getSeqTracks(workflowStep) >> seqTracks
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }

        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileViewByPidPathAsPath(_) >> { DataFile file ->
                return CreateFileHelper.createFile(tempDir.resolve(file.fileName))
            }
        }
        job.fileService = Mock(FileService) {
            _ * toFile(_) >> { Path path ->
                new File(path.toString())
            }
        }

        when:
        job.check(workflowStep)

        then:
        notThrown(WorkflowException)

        where:
        name              | mateFile1 | file1                        | mateFile2 | file2
        "given as R1, R2" | 1         | "DataFileFileName_123_R1.gz" | 2         | "DataFileFileName_123_R2.gz"
        "given as R2, R1" | 2         | "DataFileFileName_123_R2.gz" | 1         | "DataFileFileName_123_R1.gz"
    }

    void "test check, fails because seqTrack has no dataFiles"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [createSeqTrack()]

        PanCancerConditionalFailJob job = Spy(PanCancerConditionalFailJob) {
            getSeqTracks(workflowStep) >> seqTracks
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("has no dataFiles")
    }

    void "test check, fails because a seqTrack has not exactly two dataFiles"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [createSeqTrackWithOneDataFile()]

        PanCancerConditionalFailJob job = Spy(PanCancerConditionalFailJob) {
            getSeqTracks(workflowStep) >> seqTracks
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("has not exactly two dataFiles")
    }

    void "test check, fails because physical files are missing for a seqTrack"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [
                createSeqTrackWithTwoDataFile([:], [fileName: "DataFileFileName_123_R1.gz"], [fileName: "DataFileFileName_123_R2.gz"]),
                createSeqTrackWithTwoDataFile(),
        ]

        PanCancerConditionalFailJob job = Spy(PanCancerConditionalFailJob) {
            getSeqTracks(workflowStep) >> seqTracks
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }

        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileViewByPidPathAsPath(_) >> TestCase.uniqueNonExistentPath.toPath()
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are missing")
    }

    void "test check, fails when file names are not consistent for a seqTrack"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [
                createSeqTrackWithTwoDataFile([:], [fileName: "A_R1.gz"], [fileName: "B_R2.gz"]),
                createSeqTrackWithTwoDataFile([:], [fileName: "DataFileFileName_123_R1.gz"], [fileName: "DataFileFileName_123_R2.gz"]),
        ]

        PanCancerConditionalFailJob job = Spy(PanCancerConditionalFailJob) {
            getSeqTracks(workflowStep) >> seqTracks
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }

        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileViewByPidPathAsPath(_) >> { DataFile file ->
                return CreateFileHelper.createFile(tempDir.resolve(file.fileName))
            }
        }
        job.fileService = Mock(FileService) {
            _ * toFile(_) >> { Path path ->
                new File(path.toString())
            }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("file name inconsistency")
    }

    void "test check, fails whith multiple errors in one exception"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [
                createSeqTrack(),
                createSeqTrackWithOneDataFile(),
                createSeqTrackWithTwoDataFile([:], [fileName: "A_R1.gz"], [fileName: "A_R2.gz"]),
                createSeqTrackWithTwoDataFile([:], [fileName: "DataFileFileName_123_R1.gz"], [fileName: "DataFileFileName_123_R2.gz"]),
        ]

        PanCancerConditionalFailJob job = Spy(PanCancerConditionalFailJob) {
            getSeqTracks(workflowStep) >> seqTracks
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_) >> FileSystems.default
        }

        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileViewByPidPathAsPath(_) >> { DataFile file ->
                return CreateFileHelper.createFile(tempDir.resolve(file.fileName), "")
            }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are missing")
        e.message.contains("has not exactly two dataFiles")
        e.message.contains("has no dataFiles")
    }
}
