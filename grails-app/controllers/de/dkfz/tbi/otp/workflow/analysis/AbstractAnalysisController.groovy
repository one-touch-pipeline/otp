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
package de.dkfz.tbi.otp.workflow.analysis

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.AbstractAnalysisResultsService
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.PlotType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils

import java.nio.file.Path

@Slf4j
@PreAuthorize('isFullyAuthenticated()')
abstract class AbstractAnalysisController {
    ProjectSelectionService projectSelectionService
    AbstractAnalysisService abstractAnalysisService

    static allowedMethods = [
            results       : "GET",
            viewConfigFile: "GET",
    ]

    Map results() {
        return [:]
    }

    protected Map getDataTableResultsFromService(AbstractAnalysisResultsService service, Map dataToRender) {
        LogUsedTimeUtils.logUsedTimeStartEnd(log, "analysis dataTableSource") {
            Project project = projectSelectionService.requestedProject
            List data = service.getCallingInstancesForProject(project)

            dataToRender.iTotalRecords = data.size()
            dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
            dataToRender.aaData = data

            dataToRender.archived = (project.state == Project.State.ARCHIVED)
            dataToRender.deleted = (project.state == Project.State.DELETED)
        }
        return dataToRender
    }

    def viewConfigFile(AnalysisConfigFileCommand cmd) {
        Path configPath = abstractAnalysisService.fetchConfigPath(cmd.analysisInstance)

        if (!configPath) {
            return render(text: "No config file available", contentType: "text/plain")
        }

        byte[] content = configPath.bytes
        if (cmd.to == 'DOWNLOAD') {
            response.setHeader("Content-disposition", "attachment; filename=${configPath.fileName}")
        }
        render(contentType: "text/xml", file: content)
    }
}

class AnalysisConfigFileCommand {
    BamFilePairAnalysis analysisInstance
    String to
}

@ToString
class BamFilePairAnalysisPlotCommand {
    BamFilePairAnalysis bamFilePairAnalysis
    PlotType plotType
    int index

    static constraints = {
        bamFilePairAnalysis nullable: false
        plotType nullable: false
    }
}
