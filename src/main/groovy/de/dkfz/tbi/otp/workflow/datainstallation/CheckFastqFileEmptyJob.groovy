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

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflow.jobs.AbstractConditionalSkipJob
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Checks whether any of the raw sequence files for a SeqTrack contain empty content after decompression.
 * Sets the {@link RawSequenceFile#emptyFile} flag for any empty file found.
 *
 * The check uses a remote bash command to avoid loading the entire file into memory and to support
 * remote file systems where Java cannot open gzip files directly.
 */
@Component
@Slf4j
class CheckFastqFileEmptyJob extends AbstractConditionalSkipJob implements DataInstallationShared {

    private final RemoteShellHelper remoteShellHelper

    CheckFastqFileEmptyJob(RemoteShellHelper remoteShellHelper) {
        this.remoteShellHelper = remoteShellHelper
    }

    @Override
    void checkRequirements(WorkflowStep workflowStep) throws SkipWorkflowStepException {
        SeqTrack seqTrack = getSeqTrack(workflowStep)

        seqTrack.sequenceFiles.each { RawSequenceFile rawSequenceFile ->
            String path = rawSequenceFile.fullInitialPath
            String escapedPath = path.replace("'", "'\\''")
            ProcessOutput result = remoteShellHelper.executeCommandReturnProcessOutput("gzip -l '${escapedPath}'")
            if (result.exitCode != 0) {
                logService.addSimpleLogEntry(workflowStep,
                        "Cannot check emptiness of '${rawSequenceFile.fileName}' (gzip -l exited ${result.exitCode}): ${result.stderr?.trim()}")
                return
            }
            if (result.stderr) {
                logService.addSimpleLogEntry(workflowStep, "gzip -l produced warnings for '${rawSequenceFile.fileName}': ${result.stderr.trim()}")
            }

            List<String> lines = result.stdout.readLines()
            String uncompressedSize = lines.size() > 1 ? lines[1].trim().split(/\s+/)[1] : ""

            if (uncompressedSize == "0") {
                rawSequenceFile.emptyFile = true
                rawSequenceFile.save(flush: true)
                logService.addSimpleLogEntry(workflowStep, "RawSequenceFile '${rawSequenceFile.fileName}' is empty")
            }
        }
    }
}
