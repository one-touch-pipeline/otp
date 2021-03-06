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
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.jobs.AbstractConditionalFailJob
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystem
import java.nio.file.Path

@Component
@Slf4j
@CompileStatic
class DataInstallationConditionalFailJob extends AbstractConditionalFailJob implements DataInstallationShared {

    @Override
    protected void check(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        List<DataFile> dataFiles = seqTrack.dataFiles
        if (!dataFiles) {
            throw new WorkflowException("SeqTrack '${seqTrack}' has no dataFiles")
        }

        FileSystem fs = getFileSystem(workflowStep)

        final Collection<Path> missingPaths = dataFiles.collect { DataFile file ->
            lsdfFilesService.getFileInitialPathAsPath(file, fs)
        }.findAll { Path path ->
            !FileService.isFileReadableAndNotEmpty(path)
        }
        if (missingPaths) {
            throw new WorkflowException("The following ${missingPaths.size()} files are missing:\n${missingPaths.join("\n")}")
        }
    }
}
