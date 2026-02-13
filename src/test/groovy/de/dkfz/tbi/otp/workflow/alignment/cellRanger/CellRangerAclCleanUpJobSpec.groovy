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
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerFileNames
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Paths

class CellRangerAclCleanUpJobSpec extends Specification {
    CellRangerAclCleanUpJob cellRangerAclCleanUpJob
    CellRangerWorkFileService cellRangerWorkFileService
    RemoteShellHelper remoteShellHelper

    void setup() {
        cellRangerWorkFileService = Mock(CellRangerWorkFileService)
        remoteShellHelper = Mock(RemoteShellHelper) {
            0 * _
        }
        cellRangerAclCleanUpJob = new CellRangerAclCleanUpJob(cellRangerWorkFileService, remoteShellHelper)
    }

    CellRangerAclCleanUpJob overrideCellRangerJob(SingleCellBamFile bamFile) {
        return new CellRangerAclCleanUpJob(cellRangerWorkFileService, remoteShellHelper) {
            @Override
            SingleCellBamFile getBamFile(WorkflowStep ws) {
                return bamFile
            }
        }
    }

    @Unroll
    void "test execute calls getBamFile and fixCellRangerChgrpProblem"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)
        SingleCellBamFile bamFile = Mock(SingleCellBamFile)
        cellRangerAclCleanUpJob = overrideCellRangerJob(bamFile)
        String analysisDir = CellRangerFileNames.ANALYSIS_DIRECTORY_NAME
        String expectedCommandToExecute = """\
        cd ${testPath}

        mv ${analysisDir} _${analysisDir}

        cp -r _${analysisDir} ${analysisDir}

        rm -rf _${analysisDir}"""

        when:
        cellRangerAclCleanUpJob.execute(workflowStep)

        then:
        1 * remoteShellHelper.executeCommandReturnProcessOutput(expectedCommandToExecute) >> new ProcessOutput("", "", 0)
        1 * cellRangerWorkFileService.getResultDirectory(bamFile) >> testPath

        where:
        testPath << [
                Paths.get("/simple/path"),
                Paths.get("/very/deep/nested/directory/structure/for/testing"),
                Paths.get("/root"),
        ]
    }

    void "test execute with null bamFile calls service with null"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)
        cellRangerAclCleanUpJob = new CellRangerAclCleanUpJob(cellRangerWorkFileService, null) {
            @Override
            SingleCellBamFile getBamFile(WorkflowStep ws) {
                return null
            }
        }

        when:
        cellRangerAclCleanUpJob.execute(workflowStep)

        then:
        1 * cellRangerWorkFileService.getResultDirectory(null) >> {
            throw new NullPointerException() // codenarc-disable-line ThrowNullPointerException
        }
        thrown(NullPointerException)
    }
}
