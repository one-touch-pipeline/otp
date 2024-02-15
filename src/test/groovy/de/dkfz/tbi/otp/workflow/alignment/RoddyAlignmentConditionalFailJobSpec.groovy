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
package de.dkfz.tbi.otp.workflow.alignment

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.PanCancerWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystems
import java.nio.file.Path

class RoddyAlignmentConditionalFailJobSpec extends Specification implements DataTest, PanCancerWorkflowDomainFactory {

    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                RawSequenceFile,
                WorkflowStep,
        ]
    }

    @Unroll
    void "test check #name, succeeds"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        List<SeqTrack> seqTracks = [
                createSeqTrackWithTwoFastqFile([:], [mateNumber: mateFile1, fileName: file1], [mateNumber: mateFile2, fileName: file2]),
                createSeqTrackWithTwoFastqFile([:], [fileName: "SecondSeqTrack_123_R1.gz"], [fileName: "SecondSeqTrack_123_R2.gz"]),
        ]

        RoddyAlignmentConditionalFailJob job = new RoddyAlignmentConditionalFailJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> { RawSequenceFile file -> return CreateFileHelper.createFile(tempDir.resolve(file.fileName)) }
        }
        job.configService = Mock(ConfigService)
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
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
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        List<SeqTrack> seqTracks = [createSeqTrack()]

        RoddyAlignmentConditionalFailJob job = new RoddyAlignmentConditionalFailJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("has no dataFiles")
    }

    void "test check, succeeds when a seqTrack with single seqType has only one dataFile"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        List<SeqTrack> seqTracks = [createSeqTrackWithOneFastqFile([seqType: createSeqTypeSingle()])]

        RoddyAlignmentConditionalFailJob job = new RoddyAlignmentConditionalFailJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> { RawSequenceFile file -> return CreateFileHelper.createFile(tempDir.resolve(file.fileName)) }
        }
        job.configService = Mock(ConfigService)
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        notThrown(WorkflowException)
    }

    void "test check, fails because physical files are missing for a seqTrack"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        List<SeqTrack> seqTracks = [
                createSeqTrackWithTwoFastqFile([:], [fileName: "DataFileFileName_123_R1.gz"], [fileName: "DataFileFileName_123_R2.gz"]),
                createSeqTrackWithTwoFastqFile(),
        ]

        RoddyAlignmentConditionalFailJob job = new RoddyAlignmentConditionalFailJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> TestCase.uniqueNonExistentPath.toPath()
        }
        job.configService = Mock(ConfigService)
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are missing")
    }

    void "test check, fails when file names are not consistent for a seqTrack"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        List<SeqTrack> seqTracks = [
                createSeqTrackWithTwoFastqFile([:], [fileName: "A_R1.gz"], [fileName: "B_R2.gz"]),
                createSeqTrackWithTwoFastqFile([:], [fileName: "DataFileFileName_123_R1.gz"], [fileName: "DataFileFileName_123_R2.gz"]),
        ]

        RoddyAlignmentConditionalFailJob job = new RoddyAlignmentConditionalFailJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> { RawSequenceFile file ->
                return CreateFileHelper.createFile(tempDir.resolve(file.fileName))
            }
        }
        job.configService = Mock(ConfigService)
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("file name inconsistency")
    }

    void "test check, fails with multiple errors in one exception"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])
        SeqTrack seqTrack = createSeqTrack([seqType: createSeqTypeSingle()])
        List<SeqTrack> seqTracks = [
                seqTrack,
                createSeqTrack(),
                createSeqTrackWithOneFastqFile([seqType: createSeqTypePaired()]),
                createSeqTrackWithTwoFastqFile([:], [fileName: "A_R1.gz"], [fileName: "A_R2.gz"]),
                createSeqTrackWithTwoFastqFile([:], [fileName: "DataFileFileName_123_R1.gz"], [fileName: "DataFileFileName_123_R2.gz"]),
        ]
        createFastqFile([seqTrack: seqTrack])
        createFastqFile([seqTrack: seqTrack])
        RoddyAlignmentConditionalFailJob job = new RoddyAlignmentConditionalFailJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> { RawSequenceFile file -> return CreateFileHelper.createFile(tempDir.resolve(file.fileName), "") }
        }
        job.configService = Mock(ConfigService)
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are missing")
        e.message.contains("has not exactly one dataFile")
        e.message.contains("has not exactly two dataFiles")
        e.message.contains("has no dataFiles")
    }
}
