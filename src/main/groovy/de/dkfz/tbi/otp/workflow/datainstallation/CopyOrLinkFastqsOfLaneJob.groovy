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
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteClusterPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class CopyOrLinkFastqsOfLaneJob extends AbstractExecuteClusterPipelineJob implements DataInstallationShared {

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    protected List<String> createScripts(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        return createCopyJob(seqTrack)
    }

    private List<String> createCopyJob(SeqTrack seqTrack) {
        return seqTrack.sequenceFiles.collect { RawSequenceFile rawSequenceFile ->
            Path source = lsdfFilesService.getFileInitialPathAsPath(rawSequenceFile)
            Path destination = lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile)

            String md5SumFileName = checksumFileService.md5FileName(rawSequenceFile)
            return """
                |cd ${destination.parent}
                |if [ -e "${destination}" ]; then
                |    echo "File ${destination} already exists."
                |    rm ${destination}*
                |fi
                |cp ${source} ${destination}
                |md5sum ${destination.fileName} > ${md5SumFileName}
                |chgrp -h ${seqTrack.project.unixGroup} ${destination} ${md5SumFileName}
                |chmod 440 ${destination} ${md5SumFileName}
                |""".stripMargin()
        }
    }
}
