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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractConditionalFailJob
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Slf4j
@Component
class CellRangerConditionalFailJob extends AbstractConditionalFailJob implements CellRangerShared {

    @Autowired
    FileService fileService

    @Autowired
    RawSequenceDataViewFileService rawSequenceDataViewFileService

    /**
     * Check that:
     *      - input FASTQ files in viewByPid folders exist and are readable
     *      - SeqTracks are compatible with CellRanger workflow
     *
     * Note: CellRanger-compatible SeqTypes (10x_scRNA) are paired-end by definition,
     * so no additional mate count or filename validation is needed.
     *
     * @param workflowStep to check
     */
    @Override
    void check(WorkflowStep workflowStep) {
        List<SeqTrack> seqTracks = getSeqTracks(workflowStep)
        List<String> errorMessages = []

        seqTracks.each { SeqTrack seqTrack ->
            // Validate file existence
            validateFilesExist(seqTrack, errorMessages)

            // Ensure SeqTrack is compatible with CellRanger (same check as old CellRangerStartJob)
            if (!(seqTrack.seqType in SeqTypeService.cellRangerAlignableSeqTypes)) {
                errorMessages.push("SeqTrack '${seqTrack}' has incompatible SeqType '${seqTrack.seqType}' for CellRanger workflow." as String)
            }
        }

        if (errorMessages) {
            throw new WorkflowException(errorMessages.join('\n'))
        }
    }

    /**
     * Validates that all files for a SeqTrack exist and are readable.
     *
     * @param seqTrack the SeqTrack to validate
     * @param errorMessages list to collect error messages
     */
    private void validateFilesExist(final SeqTrack seqTrack, List<String> errorMessages) {
        List<RawSequenceFile> nonIndexRawSequenceFiles = seqTrack.sequenceFilesWhereIndexFileIsFalse.sort {
            it.mateNumber
        }

        if (!nonIndexRawSequenceFiles) {
            errorMessages.push("SeqTrack '${seqTrack}' has no dataFiles." as String)
            return
        }

        final Collection<Path> paths = nonIndexRawSequenceFiles.collect { RawSequenceFile rawSequenceFile ->
            rawSequenceDataViewFileService.getFilePath(rawSequenceFile)
        }

        final Collection<Path> missingPaths = paths.findAll { Path path ->
            !fileService.isFileReadableAndNotEmpty(path)
        }

        if (missingPaths) {
            errorMessages.push("The following ${missingPaths.size()} files are either missing, not readable, or empty:\n${missingPaths.join("\n")}" as String)
        }
    }
}
