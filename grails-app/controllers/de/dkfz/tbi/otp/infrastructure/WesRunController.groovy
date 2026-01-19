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
package de.dkfz.tbi.otp.infrastructure

import grails.validation.Validateable
import org.apache.http.Consts
import org.apache.http.entity.ContentType
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.WorkflowRunService
import de.dkfz.tbi.otp.workflowExecution.wes.*

import javax.servlet.http.HttpServletResponse
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class WesRunController {
    WorkflowRunService workflowRunService
    WesRunService wesRunService

    static final String NOT_AVAILABLE = "N/A"

    static allowedMethods = [
            show      : "GET",
            showTask  : "GET",
            showReport: "GET",
    ]

    def show(NavigationCommand navigationCommand) {
        WesRun wesRun = WesRun.get(params.id as long)

        return [
                'wesRun': wesRun,
                'nav'   : navigationCommand,
                'NA'    : NOT_AVAILABLE,
        ]
    }

    def showTask(NavigationCommand navigationCommand) {
        WesLog wesLog = WesLog.get(params.id as long)
        WesRun wesRun = CollectionUtils.exactlyOneElement(WesRun.findAllByWesRunLog(wesLog.wesRunLog))

        return [
                wesLog: wesLog,
                wesRun: wesRun,
                nav   : navigationCommand,
                NA    : NOT_AVAILABLE,
        ]
    }

    /**
     * Render the report file of a WES run.
     * @param reportCommand the command object containing the WES run ID
     * @return the content of the report file
     */
    def showReport(WesRunReportCommand reportCommand) {
        WesRun wesRun = wesRunService.getById(reportCommand.id)
        if (!wesRun) {
            log.error("WesRun with id ${reportCommand.id} not found")
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "WesRun not found")
            return
        }

        try {
            byte[] content = wesRunService.getReportFileContent(wesRun)
            render(file: content, contentType: ContentType.TEXT_HTML.mimeType, encoding: Consts.UTF_8)
        } catch (NoSuchFileException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Report file not found")
        } catch (AccessDeniedException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to show report file")
        }
    }
}

/**
 * Command object for WES run report requests.
 */
class WesRunReportCommand implements Validateable {
    Long id

    static constraints = {
        id(nullable: false)
    }
}
