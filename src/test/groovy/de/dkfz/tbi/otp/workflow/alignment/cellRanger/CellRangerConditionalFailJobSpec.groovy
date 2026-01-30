/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class CellRangerConditionalFailJobSpec extends Specification implements DataTest, CellRangerFactory, WorkflowSystemDomainFactory {

    CellRangerConditionalFailJob job = new CellRangerConditionalFailJob()

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                RawSequenceFile,
                SeqType,
                SeqTrack,
                WorkflowStep,
        ]
    }

    @TempDir
    Path tempDir

    WorkflowStep workflowStep
    SeqType cellRangerSeqType

    void setup() {
        // Create the exact 10x_scRNA SeqType using the CellRangerFactory properties
        // This ensures it matches what SeqTypeService.get10xSingleCellRnaSeqType() expects
        cellRangerSeqType = createSeqType(seqTypeProperties)

        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateWorkflow(CellRangerWorkflow.WORKFLOW, [
                                beanName: CellRangerWorkflow.simpleName.uncapitalize()
                        ]),
                ]),
        ])
    }

    void "test check, succeeds with valid CellRanger SeqTracks and existing files"() {
        given: "CellRanger-compatible SeqTracks with FASTQ files"
        List<SeqTrack> seqTracks = [
                createSeqTrackWithOneFastqFile([seqType: cellRangerSeqType], [mateNumber: 1]),
                createSeqTrackWithOneFastqFile([seqType: cellRangerSeqType], [mateNumber: 1]),
        ]

        and: "A CellRangerConditionalFailJob with mocked services"
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, AlignmentWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> { RawSequenceFile file ->
                return CreateFileHelper.createFile(tempDir.resolve(file.fileName))
            }
        }
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        notThrown(WorkflowException)
    }

    void "test check, fails because seqTrack has no dataFiles"() {
        given: "A SeqTrack with no FASTQ files"
        List<SeqTrack> seqTracks = [createSeqTrack([seqType: cellRangerSeqType])]

        and: "A CellRangerConditionalFailJob with mocked services"
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, AlignmentWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("has no dataFiles")
    }

    void "test check, fails because physical files are missing"() {
        given: "SeqTracks with FASTQ files that don't exist on disk"
        List<SeqTrack> seqTracks = [
                createSeqTrackWithOneFastqFile([seqType: cellRangerSeqType], [mateNumber: 1]),
                createSeqTrackWithOneFastqFile([seqType: cellRangerSeqType], [mateNumber: 1]),
        ]

        and: "A CellRangerConditionalFailJob that returns non-existent paths"
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, AlignmentWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> TestCase.uniqueNonExistentPath.toPath()
        }
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are either missing, not readable, or empty")
    }

    void "test check, fails because seqTrack has incompatible SeqType"() {
        given: "SeqTracks with non-CellRanger compatible SeqType"
        SeqType incompatibleSeqType = createSeqType([
                name         : SeqTypeNames.WHOLE_GENOME.seqTypeName,
                displayName  : "WGS",
                dirName      : "whole_genome_sequencing",
                libraryLayout: SequencingReadType.PAIRED,
                singleCell   : false,
        ])
        List<SeqTrack> seqTracks = [createSeqTrackWithOneFastqFile([seqType: incompatibleSeqType], [mateNumber: 1])]

        and: "A CellRangerConditionalFailJob with mocked services"
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, AlignmentWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> { RawSequenceFile file ->
                return CreateFileHelper.createFile(tempDir.resolve(file.fileName))
            }
        }
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("incompatible SeqType")
        e.message.contains("CellRanger workflow")
    }

    void "test check, fails with multiple errors combined"() {
        given: "Mixed SeqTrack scenarios: no files, incompatible type, and missing files"
        SeqType incompatibleSeqType = createSeqType([
                name         : SeqTypeNames.WHOLE_GENOME.seqTypeName,
                displayName  : "WGS",
                dirName      : "whole_genome_sequencing",
                libraryLayout: SequencingReadType.PAIRED,
                singleCell   : false,
        ])

        List<SeqTrack> seqTracks = [
                createSeqTrack([seqType: cellRangerSeqType]), // No files
                createSeqTrackWithOneFastqFile([seqType: incompatibleSeqType], [mateNumber: 1]), // Wrong type + missing files
                createSeqTrackWithOneFastqFile([seqType: cellRangerSeqType], [mateNumber: 1]), // Valid but missing files
        ]

        and: "A CellRangerConditionalFailJob returning non-existent paths"
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, AlignmentWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> TestCase.uniqueNonExistentPath.toPath()
        }
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("has no dataFiles")
        e.message.contains("incompatible SeqType")
        e.message.contains("files are either missing, not readable, or empty")
    }

    void "test check, fails when files exist but are empty"() {
        given: "SeqTracks with FASTQ files that exist but are empty"
        List<SeqTrack> seqTracks = [createSeqTrackWithOneFastqFile([seqType: cellRangerSeqType], [mateNumber: 1])]

        and: "A CellRangerConditionalFailJob creating empty files"
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefacts(workflowStep, AlignmentWorkflow.INPUT_FASTQ) >> seqTracks
            0 * _
        }

        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePath(_) >> { RawSequenceFile file ->
                return CreateFileHelper.createFile(tempDir.resolve(file.fileName), "")
            }
        }
        job.fileService = new FileService()
        job.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are either missing, not readable, or empty")
    }
}
