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

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.dataprocessing.BamImportInstance
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

@Component
@Slf4j
class BamImportReplaceSourceJob extends AbstractLinkJob implements BamImportShared {

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        List<LinkEntry> linkEntries = []
        ExternallyProcessedBamFile bamFile = getBamFile(workflowStep)
        BamImportInstance importInstance = getImportInstance(bamFile)

        if (importInstance.linkOperation.replaceSourceWithLink) {
            Path linkBam = externallyProcessedBamFileService.getSourceBamFilePath(bamFile)
            Path linkBai = externallyProcessedBamFileService.getSourceBaiFilePath(bamFile)
            Path linkBaseDir = externallyProcessedBamFileService.getSourceBaseDirFilePath(bamFile)

            Path targetBamFile = externallyProcessedBamFileService.getBamFile(bamFile, PathOption.REAL_PATH)
            Path targetBaiFile = externallyProcessedBamFileService.getBaiFile(bamFile, PathOption.REAL_PATH)
            Path targetBaseDir = externallyProcessedBamFileService.getImportFolder(bamFile, PathOption.REAL_PATH)

            linkEntries.add(new LinkEntry(link: linkBam, target: targetBamFile))
            linkEntries.add(new LinkEntry(link: linkBai, target: targetBaiFile))

            bamFile.furtherFiles.each { String relativePath ->
                Path absoluteLinkPath = linkBaseDir.resolve(relativePath)
                if (Files.isDirectory(absoluteLinkPath)) {
                    fileService.deleteDirectoryRecursively(absoluteLinkPath)
                }
                linkEntries.add(new LinkEntry(
                        link: absoluteLinkPath,
                        target: targetBaseDir.resolve(relativePath)
                ))
            }
        }
        return linkEntries
    }

    @Override
    protected void doFurtherWork(WorkflowStep workflowStep) { }

    @Override
    protected void saveResult(WorkflowStep workflowStep) { }
}
