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
package de.dkfz.tbi.otp.workflowExecution.wes

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor
import io.swagger.client.wes.model.State
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

@Transactional
class WesRunService {

    FileService fileService
    FilestoreService filestoreService

    // regex to find the report file (starting with 'report' and ending with '.html'. e.g. report-20252034.html)
    static final String MATCHER_REPORT_FILE = /report.*\.html/

    // mapping to determine the highest priority of Weskit run check states
    static final Map<WesRun.MonitorState, Integer> MAPPING_CHECK_STATES = [
            (WesRun.MonitorState.CHECKING): 3,
            (WesRun.MonitorState.FINISHED): 1,
    ].asImmutable()

    /**
     * returns all Run of weskit currently checked by OTP
     */
    @CompileDynamic
    List<WesRun> monitoredRuns() {
        return WesRun.findAllByState(WesRun.MonitorState.CHECKING)
    }

    @CompileDynamic
    List<WesRun> allByWorkflowStep(WorkflowStep workflowStep) {
        return WesRun.findAllByWorkflowStep(workflowStep)
    }

    /**
     * Store the WesRun into database.
     * The run state will be set as WesRun.MonitorState.CHECKING
     *
     * @param workflowStep current workflow step
     * @param wesIdentifier ID of the wes run
     * @param subPath last part of the path
     */
    void saveWorkflowRun(WorkflowStep workflowStep, String wesIdentifier, String subPath) {
        WesRun wesRun = new WesRun(
                workflowStep: workflowStep,
                wesIdentifier: wesIdentifier,
                subPath: subPath, // only the last level of the path
                state: WesRun.MonitorState.CHECKING)
        wesRun.save(flush: true)
    }

    /**
     * Get the report file path of the given WesRun (only the first found).
     * The report file is searched in the sub path of the WesRun.
     * Note: This method could be slow, because it searches the remote file system.
     *
     * @param wesRun the given wesRun
     * @return the path to the report file if found, otherwise null
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Path getReportPath(WesRun wesRun) {
        Path workFolder = filestoreService.getWorkFolderPath(wesRun.workflowStep.workflowRun.workFolder)
        Path subPath = workFolder.resolve(wesRun.subPath)

        log.debug("Searching for report file in path ${subPath} with matcher ${MATCHER_REPORT_FILE}")

        return fileService.findFileInPath(subPath, MATCHER_REPORT_FILE)
    }

    /**
     * Get the report file content of the given WesRun.
     * The user must have read access as an operator only.
     *
     * @param wesRun WesRun which the report file belongs to
     * @return byte array of the report file
     * @throws NoSuchFileException if the report file does not exist
     * @throws AccessDeniedException if the report file is not readable
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    byte[] getReportFileContent(WesRun wesRun) throws NoSuchFileException, AccessDeniedException {
        Path reportPath = getReportPath(wesRun)
        log.debug("Reading report file at path ${reportPath} for WesRun ${wesRun}")
        if (!reportPath || !Files.exists(reportPath)) {
            log.error("Report file for ${wesRun.id} not found: ${reportPath ?: wesRun.subPath}")
            throw new NoSuchFileException("Report file for ${wesRun.id} not found: ${reportPath ?: wesRun.subPath}")
        }
        if (!fileService.fileIsReadable(reportPath)) {
            log.error("Report file not readable at path ${reportPath.toAbsolutePath()}")
            throw new AccessDeniedException("Report file for ${wesRun.id} found but not readable: ${reportPath}")
        }
        return Files.readAllBytes(reportPath)
    }

    /**
     * Get the WesRun by its id.
     *
     * @param id is the ID of the WesRun
     * @return WesRun or null if not found
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    WesRun getById(Long id) {
        return WesRun.get(id)
    }

    String getCumulatedWesRunsStatus(List<WesRunStateDto> wesRunStates) {
        if (!wesRunStates) {
            return ''
        }

        WesRunStateDto highestPriorityState = wesRunStates.max { wesRunState ->
            (MAPPING_CHECK_STATES[wesRunState.monitorStatus] ?: 0) + (RunStatusService.END_STATES[wesRunState.wesRunState] ?: 0)
        }
        State displayState = highestPriorityState.wesRunState ?: State.UNKNOWN

        return "${highestPriorityState.monitorStatus}${highestPriorityState.monitorStatus == WesRun.MonitorState.FINISHED ? "/${displayState}" : ""}"
    }

    boolean hasReports(WesRun wesRun) {
        return !wesRun.workflowStep.obsolete && (
                wesRun.wesRunLog?.state == State.COMPLETE || wesRun.wesRunLog?.state == State.EXECUTOR_ERROR || wesRun.wesRunLog?.state == State.SYSTEM_ERROR
        )
    }
}

@TupleConstructor
class WesRunStateDto {
    WesRun.MonitorState monitorStatus
    State wesRunState
}
