/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.bamImport

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.nio.file.Path

@Component
@Slf4j
class BamImportLinkSourceJob extends AbstractLinkJob implements BamImportShared {

    @Autowired
    ExternallyProcessedBamFileService externallyProcessedBamFileService

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        List<LinkEntry> linkEntries = []

        ExternallyProcessedBamFile bamFile = getBamFile(workflowStep)

        ImportProcess importProcess = getImportProcess(bamFile)

        List<String> fileNames = [
                bamFile.fileName,
                bamFile.baiFileName,
        ]
        fileNames.addAll(bamFile.furtherFiles)

        Path importFolder = externallyProcessedBamFileService.getImportFolder(bamFile, PathOption.REAL_PATH)
        Path sourceFolder = externallyProcessedBamFileService.getSourceBaseDirFilePath(bamFile)

        return importProcess.linkOperation.linkSource ? fileNames.collect {
            new LinkEntry(
                    link: importFolder.resolve(it),
                    target: sourceFolder.resolve(it),
            )
        } : linkEntries
    }

    @Override
    protected void doFurtherWork(WorkflowStep workflowStep) { }

    @Override
    protected void saveResult(WorkflowStep workflowStep) { }
}
