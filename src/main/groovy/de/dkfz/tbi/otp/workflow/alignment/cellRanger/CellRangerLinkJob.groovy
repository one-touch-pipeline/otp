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
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Slf4j
@Component
class CellRangerLinkJob extends AbstractLinkJob implements CellRangerShared {

    private final CellRangerWorkFileService cellRangerWorkFileService
    private final CellRangerLinkFileService cellRangerLinkFileService

    CellRangerLinkJob(CellRangerWorkFileService cellRangerWorkFileService, CellRangerLinkFileService cellRangerLinkFileService) {
        this.cellRangerWorkFileService = cellRangerWorkFileService
        this.cellRangerLinkFileService = cellRangerLinkFileService
    }

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        SingleCellBamFile bamFile = getBamFile(workflowStep)
        List<LinkEntry> links = []

        Path resultDir = cellRangerWorkFileService.getResultDirectory(bamFile)
        Path linkDir = cellRangerLinkFileService.getDirectoryPath(bamFile)

        cellRangerWorkFileService.getFileMappingForLinks(bamFile).each { String linkName, String targetName ->
            links.add(new LinkEntry(
                    link: linkDir.resolve(linkName),
                    target: resultDir.resolve(targetName)
            ))
        }
        return links
    }

    @Override
    protected void doFurtherWork(WorkflowStep workflowStep) {
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
    }
}
