/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelResultsService
import de.dkfz.tbi.otp.utils.DataTableCommand

import java.nio.file.Path

@PreAuthorize('isFullyAuthenticated()')
class IndelController extends AbstractAnalysisController {

    IndelResultsService indelResultsService

    static allowedMethods = [
            plots           : "GET",
            renderPlot      : "GET",
            dataTableResults: "POST",
    ]

    Map plots(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(404)
        }
        return indelResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType) ? [
                id      : cmd.bamFilePairAnalysis.id,
                pid     : cmd.bamFilePairAnalysis.individual.pid,
                plotType: cmd.plotType,
                error   : null,
        ] : [
                error: "File not found",
                pid  : "no File",
        ]
    }

    def renderPlot(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(404)
        }
        List<Path> filePaths = indelResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)
        Path file = filePaths.first()
        if (cmd.plotType == PlotType.INDEL) {
            render(file: file.bytes, contentType: "application/pdf")
        } else {
            render(file: file.bytes, contentType: "image/png")
        }
    }

    JSON dataTableResults(DataTableCommand cmd) {
        Map dataToRender = getDataTableResultsFromService(indelResultsService, cmd.dataToRender())
        return render(dataToRender as JSON)
    }
}
