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
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflow.jobs.AbstractConditionalSkipJob
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Flags each empty raw sequence file of a SeqTrack by decompressing the front and bailing after the
 * first byte ({@code zcat '<path>' | head -c 1 | wc -c; echo zcat_status=${PIPESTATUS[0]}}). Outcomes:
 * 0 bytes with zcat exit 0 is empty and sets {@link RawSequenceFile#emptyFile};
 * 0 bytes with zcat exit non-zero is corrupt and not flagged;
 * at least 1 byte is non-empty and not flagged;
 * a timeout is not flagged.
 */
@Component
@Slf4j
class CheckFastqFileEmptyJob extends AbstractConditionalSkipJob implements DataInstallationShared {

    /** Seconds before the emptiness check is aborted; healthy files return sub-second, so this only fires on pathological or very slow input. */
    private static final int COMMAND_TIMEOUT_SECONDS = 300

    /** Exit code GNU {@code timeout} returns when it kills the command. */
    private static final int TIMEOUT_EXIT_CODE = 124

    private static final String STATUS_PREFIX = "zcat_status="

    private final RemoteShellHelper remoteShellHelper

    CheckFastqFileEmptyJob(RemoteShellHelper remoteShellHelper) {
        this.remoteShellHelper = remoteShellHelper
    }

    @Override
    void checkRequirements(WorkflowStep workflowStep) throws SkipWorkflowStepException {
        SeqTrack seqTrack = getSeqTrack(workflowStep)

        seqTrack.sequenceFiles.each { RawSequenceFile rawSequenceFile ->
            String payload = "zcat ${LocalShellHelper.shellEscape(rawSequenceFile.fullInitialPath)} | head -c 1 | wc -c; " +
                    "echo \"${STATUS_PREFIX}\${PIPESTATUS[0]}\""
            String command = "timeout ${COMMAND_TIMEOUT_SECONDS} bash -c ${LocalShellHelper.shellEscape(payload)}"
            ProcessOutput result = remoteShellHelper.executeCommandReturnProcessOutput(command)

            if (result.exitCode == TIMEOUT_EXIT_CODE) {
                logService.addSimpleLogEntry(workflowStep, "Emptiness check for '${rawSequenceFile.fileName}' timed out after " +
                        "${COMMAND_TIMEOUT_SECONDS}s and was not flagged as empty")
                return
            }

            List<String> lines = (result.stdout ?: "").readLines()*.trim()
            Integer byteCount = lines.find { it ==~ /\d+/ }?.toInteger()
            String statusLine = lines.find { it.startsWith(STATUS_PREFIX) }
            Integer zcatStatus = statusLine ? statusLine.substring(STATUS_PREFIX.length()).toInteger() : null

            if (byteCount == null || zcatStatus == null) {
                logService.addSimpleLogEntry(workflowStep, "Cannot check emptiness of '${rawSequenceFile.fileName}' " +
                        "(unexpected output, exit ${result.exitCode}): ${(result.stdout ?: "").trim()}")
                return
            }

            if (byteCount == 0 && zcatStatus != 0) {
                logService.addSimpleLogEntry(workflowStep, "Cannot check emptiness of '${rawSequenceFile.fileName}' " +
                        "(zcat exited ${zcatStatus}): ${result.stderr?.trim()}")
                return
            }

            if (byteCount == 0) {
                rawSequenceFile.emptyFile = true
                rawSequenceFile.save(flush: true)
                logService.addSimpleLogEntry(workflowStep, "RawSequenceFile '${rawSequenceFile.fileName}' is empty")
            }
        }
    }
}
