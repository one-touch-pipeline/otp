/*
 * Copyright 2011-2020 The OTP authors
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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.jobs.AbstractOtpClusterValidationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

@Component
@Slf4j
@CompileStatic
class DataInstallationValidationJob extends AbstractOtpClusterValidationJob implements DataInstallationShared {

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        FileSystem fs = getFileSystem(workflowStep)

        return seqTrack.dataFiles.collect { DataFile dataFile ->
            fs.getPath(lsdfFilesService.getFileFinalPath(dataFile))
        }
    }

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected List<String> doFurtherValidationAndReturnProblems(WorkflowStep workflowStep) {
        List<String> problems = []
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        seqTrack.dataFiles.each { DataFile dataFile ->
            if (!checksumFileService.compareMd5(dataFile)) {
                problems.add("The md5sum of file ${dataFile.fileName} is not the expected ${dataFile.md5sum}" as String)
            }
        }
        return problems
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        FileSystem fs = getFileSystem(workflowStep)

        seqTrack.dataFiles.collect { DataFile dataFile ->
            Path targetPath = fs.getPath(lsdfFilesService.getFileFinalPath(dataFile))

            dataFile.fileSize = Files.size(targetPath)
            dataFile.dateFileSystem = new Date(Files.getLastModifiedTime(targetPath).toMillis())
            dataFile.fileExists = true
            assert dataFile.save(flush: true)
        }
    }
}
