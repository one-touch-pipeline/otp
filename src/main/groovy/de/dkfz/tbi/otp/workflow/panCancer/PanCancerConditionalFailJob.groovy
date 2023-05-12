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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.jobs.AbstractConditionalFailJob
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class PanCancerConditionalFailJob extends AbstractConditionalFailJob implements PanCancerShared {

    @Autowired
    FileService fileService

    /**
     * Check that:
     *      - input files in viewByPid folders exists and are readable
     *      - all seqTracks have exact two read dataFiles (ignoring index data)
     *      - the file names of each seqTrack are correct
     *      - file name order in Roddy is correct (it's a double check since it cannot be wrong ordered with exactly two dataFiles)
     *
     * @param workflowStep to check
     */
    @Override
    protected void check(WorkflowStep workflowStep) {
        List<SeqTrack> seqTracks = getSeqTracks(workflowStep)
        List<String> errorMessages = []
        List<Path> allDataFiles = []

        seqTracks.each { SeqTrack seqTrack ->
            List<DataFile> nonIndexDataFiles = seqTrack.dataFilesWhereIndexFileIsFalse.sort {
                it.mateNumber
            }

            if (!nonIndexDataFiles) {
                return errorMessages.push("SeqTrack '${seqTrack}' has no dataFiles." as String)
            }

            if (nonIndexDataFiles.size() != 2) {
                return errorMessages.push("SeqTrack '${seqTrack}' has not exactly two dataFiles. " +
                        "It has ${nonIndexDataFiles} (index files are ignored)." as String)
            }

            final Collection<Path> paths = nonIndexDataFiles.collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPathAsPath(dataFile)
            }

            final Collection<Path> missingPaths = paths.findAll { Path path ->
                !FileService.isFileReadableAndNotEmpty(path)
            }

            if (missingPaths) {
                return errorMessages.push("The following ${missingPaths.size()} files are missing:\n${missingPaths.join("\n")}" as String)
            }

            allDataFiles.addAll(paths)

            try {
                MetaDataService.ensurePairedSequenceFileNameConsistency(nonIndexDataFiles[0].fileName, nonIndexDataFiles[1].fileName)
            } catch (IllegalFileNameException e) {
                return errorMessages.push("SeqTrack '${seqTrack}' file name inconsistency:\n" + e.message as String)
            }
        }

        try {
            MetaDataService.ensurePairedSequenceFileNameOrder(allDataFiles.collect {
                fileService.toFile(it)
            })
        } catch (IllegalFileNameException e) {
            errorMessages.push("File name order of the seqTracks dataFiles is not correct:\n" + e.message)
        }

        if (errorMessages) {
            throw new WorkflowException(errorMessages.join('\n'))
        }
    }
}
