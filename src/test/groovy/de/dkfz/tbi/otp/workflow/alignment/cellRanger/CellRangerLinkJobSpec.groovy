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

import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class CellRangerLinkJobSpec extends Specification {

    CellRangerLinkJob cellRangerLinkJob
    CellRangerWorkFileService cellRangerWorkFileService
    CellRangerLinkFileService cellRangerLinkFileService

    void setup() {
        cellRangerWorkFileService = Mock(CellRangerWorkFileService)
        cellRangerLinkFileService = Mock(CellRangerLinkFileService)
    }

    CellRangerLinkJob overrideCellRangerJob(SingleCellBamFile bamFile) {
        return new CellRangerLinkJob(cellRangerWorkFileService, cellRangerLinkFileService) {
            @Override
            SingleCellBamFile getBamFile(WorkflowStep workflowStep) {
                return bamFile
            }
        }
    }

    void "test getLinkMap"() {
        given:
        SingleCellBamFile bamFile = Mock(SingleCellBamFile)
        WorkflowStep workflowStep = Mock(WorkflowStep)
        cellRangerLinkJob = overrideCellRangerJob(bamFile)

        Path resultDir = Path.of("/result/dir")
        Path linkDir = Path.of("/link/dir")

        cellRangerWorkFileService.getResultDirectory(bamFile) >> resultDir
        cellRangerLinkFileService.getDirectoryPath(bamFile) >> linkDir
        cellRangerWorkFileService.getFileMappingForLinks(bamFile) >> [
                "link1": "target1",
                "link2": "target2",
        ]

        when:
        List links = cellRangerLinkJob.getLinkMap(workflowStep)

        then:
        links.size() == 2
        links.containsAll([
                new LinkEntry(
                        link: linkDir.resolve("link1"),
                        target: resultDir.resolve("target1")
                ),
                new LinkEntry(
                        link: linkDir.resolve("link2"),
                        target: resultDir.resolve("target2")
                ),
        ])
    }
}
