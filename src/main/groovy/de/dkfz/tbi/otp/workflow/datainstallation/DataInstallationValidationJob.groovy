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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.jobs.AbstractOtpClusterValidationJob
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

@Component
@Slf4j
class DataInstallationValidationJob extends AbstractOtpClusterValidationJob implements DataInstallationShared {

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)

        return seqTrack.sequenceFiles.collect { RawSequenceFile rawSequenceFile ->
            rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
        }
    }

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void doFurtherValidation(WorkflowStep workflowStep) {
        List<String> problems = []
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        seqTrack.sequenceFiles.each { RawSequenceFile rawSequenceFile ->
            if (!checksumFileService.compareMd5(rawSequenceFile)) {
                problems.add("The md5sum of file ${rawSequenceFile.fileName} is not the expected ${(rawSequenceFile as FastqFile).fastqMd5sum}" as String)
            }
        }
        if (problems) {
            throw new ValidationJobFailedException(problems.join('\n'))
        }
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)

        seqTrack.sequenceFiles.collect { RawSequenceFile rawSequenceFile ->
            Path targetPath = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)

            rawSequenceFile.fileSize = Files.size(targetPath)
            rawSequenceFile.dateFileSystem = new Date(Files.getLastModifiedTime(targetPath).toMillis())
            rawSequenceFile.fileExists = true
            assert rawSequenceFile.save(flush: true)
        }
    }
}
