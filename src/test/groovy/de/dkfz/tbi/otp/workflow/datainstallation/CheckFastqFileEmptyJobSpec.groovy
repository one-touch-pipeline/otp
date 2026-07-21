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
        // non-empty file: head reads its byte and closes the pipe, so zcat is killed by SIGPIPE (141)
        remoteShellHelper.executeCommandReturnProcessOutput(_) >> new ProcessOutput(stdout: "1\nzcat_status=141", stderr: "", exitCode: 0)

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
        // empty file: zcat decompresses to zero bytes and exits cleanly (0)
        remoteShellHelper.executeCommandReturnProcessOutput(_) >> new ProcessOutput(stdout: "0\nzcat_status=0", stderr: "", exitCode: 0)

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
        FastqFile.get(fileId).emptyFile
    }

    void "checkRequirements should issue the timeout-guarded bash pipeline with the escaped path"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        String path = seqTrack.sequenceFiles.first().fullInitialPath
        job.concreteArtefactService.getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
        String issuedCommand = null

        when:
        job.checkRequirements(workflowStep)

        then:
        1 * remoteShellHelper.executeCommandReturnProcessOutput(_) >> { String cmd ->
            issuedCommand = cmd
            return new ProcessOutput(stdout: "1\nzcat_status=141", stderr: "", exitCode: 0)
        }
        issuedCommand == "timeout 300 bash -c 'zcat '\\''${path}'\\'' | head -c 1 | wc -c; echo \"zcat_status=\${PIPESTATUS[0]}\"'"
    }

    void "checkRequirements should not set emptyFile and should log when decompression fails"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        long fileId = seqTrack.sequenceFiles.first().id
        job.concreteArtefactService.getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
        // corrupt file: zero bytes produced but zcat errored (non-zero) before any output
        remoteShellHelper.executeCommandReturnProcessOutput(_) >> new ProcessOutput(
                stdout: "0\nzcat_status=1", stderr: "gzip: file.fastq.gz: not in gzip format", exitCode: 0)

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
        !FastqFile.get(fileId).emptyFile
    }

    void "checkRequirements should not set emptyFile and should log when the command times out"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        long fileId = seqTrack.sequenceFiles.first().id
        job.concreteArtefactService.getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
        // timeout fired: GNU timeout exits 124 and kills the command
        remoteShellHelper.executeCommandReturnProcessOutput(_) >> new ProcessOutput(stdout: "", stderr: "", exitCode: 124)

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
        !FastqFile.get(fileId).emptyFile
    }

    void "checkRequirements should set emptyFile only on empty files when results are mixed"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        List<RawSequenceFile> sortedFiles = seqTrack.sequenceFiles.sort { it.id }
        long emptyFileId = sortedFiles[0].id
        long nonEmptyFileId = sortedFiles[1].id

        job.concreteArtefactService.getOutputArtefact(workflowStep, DataInstallationWorkflow.OUTPUT_FASTQ) >> seqTrack
        remoteShellHelper.executeCommandReturnProcessOutput { String cmd -> cmd.contains(sortedFiles[0].fullInitialPath) } >>
                new ProcessOutput(stdout: "0\nzcat_status=0", stderr: "", exitCode: 0)
        remoteShellHelper.executeCommandReturnProcessOutput { String cmd -> cmd.contains(sortedFiles[1].fullInitialPath) } >>
                new ProcessOutput(stdout: "1\nzcat_status=141", stderr: "", exitCode: 0)

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
        FastqFile.get(emptyFileId).emptyFile
        !FastqFile.get(nonEmptyFileId).emptyFile
    }
}
