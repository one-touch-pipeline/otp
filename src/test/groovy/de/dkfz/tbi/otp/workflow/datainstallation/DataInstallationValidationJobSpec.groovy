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
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.DataInstallationWorkflowDomainFactory
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class DataInstallationValidationJobSpec extends Specification implements DataTest, DataInstallationWorkflowDomainFactory {

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

    void "test getExpectedFiles"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])
        DataInstallationValidationJob job = new DataInstallationValidationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(_) >> { RawSequenceFile rawSequenceFile -> rawSequenceFile.fileName }
        }
        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        when:
        List<Path> result = job.getExpectedFiles(workflowStep)

        then:
        containSame(result*.fileName*.toString(), seqTrack.sequenceFiles*.fileName)
    }

    void "test getExpectedDirectories"() {
        given:
        DataInstallationValidationJob job = new DataInstallationValidationJob()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])

        expect:
        [] == job.getExpectedDirectories(workflowStep)
    }

    @Unroll
    void "test doFurtherValidation, when md5Sum is correct, then return empty list"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])
        DataInstallationValidationJob job = new DataInstallationValidationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }
        job.checksumFileService = Mock(ChecksumFileService)

        when:
        job.doFurtherValidation(workflowStep)

        then:
        1 * job.checksumFileService.compareMd5(seqTrack.sequenceFiles.first()) >> true
        1 * job.checksumFileService.compareMd5(seqTrack.sequenceFiles.last()) >> true

        notThrown(ValidationJobFailedException)
    }

    void "test doFurtherValidation, when md5Sum is incorrect, then return list with problems"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])
        DataInstallationValidationJob job = new DataInstallationValidationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }
        job.checksumFileService = Mock(ChecksumFileService)

        when:
        job.doFurtherValidation(workflowStep)

        then:
        1 * job.checksumFileService.compareMd5(seqTrack.sequenceFiles.first()) >> false
        1 * job.checksumFileService.compareMd5(seqTrack.sequenceFiles.last()) >> false

        Exception e = thrown(ValidationJobFailedException)
        String[] messages = e.message.split('\n')
        messages.size() == 2
        messages.each {
            assert it ==~ ("The md5sum of file .* is not the expected .*")
        }
    }

    void "test saveResult"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])

        DataInstallationValidationJob job = new DataInstallationValidationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
            0 * _
        }
        job.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileFinalPath(_) >> Paths.get("")
        }

        when:
        job.saveResult(workflowStep)

        then:
        seqTrack.sequenceFiles.every { it.fileSize }
        seqTrack.sequenceFiles.every { it.dateFileSystem }
        seqTrack.sequenceFiles.every { it.fileExists }
    }
}
