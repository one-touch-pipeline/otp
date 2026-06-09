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
package de.dkfz.tbi.otp.workflow.datainstallation

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.DataInstallationWorkflowDomainFactory
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class CheckFastqFileEmptyJobSpec extends Specification implements DataTest, DataInstallationWorkflowDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                RawSequenceFile,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    CheckFastqFileEmptyJob job
    WorkflowStep workflowStep
    RemoteShellHelper remoteShellHelper

    void setup() {
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateDataInstallationWorkflowWorkflow(),
                ]),
        ])
        remoteShellHelper = Mock(RemoteShellHelper)
        job = new CheckFastqFileEmptyJob(remoteShellHelper)
        job.logService = Stub(LogService)
        job.concreteArtefactService = Mock(ConcreteArtefactService)
    }

    void "checkRequirements should not set emptyFile flag and not throw when all files are non-empty"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        job.concreteArtefactService.getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
        remoteShellHelper.executeCommandReturnProcessOutput(_) >> new ProcessOutput(
                stdout: "         compressed        uncompressed  ratio uncompressed_name\n          8          12  72.6% file.fastq.gz",
                stderr: "", exitCode: 0)

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
        FastqFile.list().every { !it.emptyFile }
    }

    void "checkRequirements should set emptyFile flag and not throw when a file is empty"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        long fileId = seqTrack.sequenceFiles.first().id
        job.concreteArtefactService.getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
        remoteShellHelper.executeCommandReturnProcessOutput(_) >> new ProcessOutput(
                stdout: "         compressed        uncompressed  ratio uncompressed_name\n         20           0   0.0% file.fastq.gz",
                stderr: "", exitCode: 0)

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
        FastqFile.get(fileId).emptyFile
    }

    void "checkRequirements should set emptyFile only on empty files when results are mixed"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        List<RawSequenceFile> sortedFiles = seqTrack.sequenceFiles.sort { it.id }
        long emptyFileId = sortedFiles[0].id
        long nonEmptyFileId = sortedFiles[1].id

        job.concreteArtefactService.getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
        remoteShellHelper.executeCommandReturnProcessOutput { String cmd -> cmd.contains(sortedFiles[0].fullInitialPath) } >>
                new ProcessOutput(
                        stdout: "         compressed        uncompressed  ratio uncompressed_name\n         20           0   0.0% file.fastq.gz",
                        stderr: "", exitCode: 0)
        remoteShellHelper.executeCommandReturnProcessOutput { String cmd -> cmd.contains(sortedFiles[1].fullInitialPath) } >>
                new ProcessOutput(
                        stdout: "         compressed        uncompressed  ratio uncompressed_name\n          8          12  72.6% file.fastq.gz",
                        stderr: "", exitCode: 0)

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
        FastqFile.get(emptyFileId).emptyFile
        !FastqFile.get(nonEmptyFileId).emptyFile
    }
}
