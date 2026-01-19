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
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerFileNames
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class CellRangerValidationJobSpec extends Specification {
    CellRangerValidationJob cellRangerValidationJob
    CellRangerWorkFileService cellRangerWorkFileService

    void setup() {
        cellRangerWorkFileService = Mock(CellRangerWorkFileService)
        cellRangerValidationJob = new CellRangerValidationJob(cellRangerWorkFileService)
    }

    CellRangerValidationJob overrideCellRangerJob(SingleCellBamFile bamFile) {
        return new CellRangerValidationJob(cellRangerWorkFileService) {
            @Override
            SingleCellBamFile getBamFile(WorkflowStep workflowStep) {
                return bamFile
            }
        }
    }

    void "test getExpectedDirectories returns correct paths"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)
        SingleCellBamFile bamFile = Mock(SingleCellBamFile)
        Path mockPath = Paths.get("/mock/result/directory")
        cellRangerValidationJob = overrideCellRangerJob(bamFile)

        List<Path> expectedDirs = CellRangerFileNames.CREATED_RESULT_DIRS.collect { String dirName ->
            mockPath.resolve(dirName)
        }
        when:
        List<Path> result = cellRangerValidationJob.getExpectedDirectories(workflowStep)

        then:
        1 * cellRangerWorkFileService.getResultDirectory(bamFile) >> mockPath
        result == expectedDirs
    }

    void "test getExpectedFiles returns correct paths"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)
        SingleCellBamFile bamFile = Mock(SingleCellBamFile)
        Path mockPath = Paths.get("/mock/result/directory")
        cellRangerValidationJob = overrideCellRangerJob(bamFile)

        List<Path> expectedFiles = CellRangerFileNames.CREATED_RESULT_FILES.collect { String fileName ->
            mockPath.resolve(fileName)
        }
        when:
        List<Path> result = cellRangerValidationJob.getExpectedFiles(workflowStep)

        then:
        1 * cellRangerWorkFileService.getResultDirectory(bamFile) >> mockPath
        result == expectedFiles
    }
}
