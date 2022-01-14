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
import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqResultsService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DataTableCommand
import java.nio.file.Path

@Secured('isFullyAuthenticated()')
class AceseqController extends AbstractAnalysisController {

    AceseqResultsService aceseqResultsService
    ProjectSelectionService projectSelectionService

    JSON dataTableResults(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        Project project = projectSelectionService.requestedProject
        List data = aceseqResultsService.getCallingInstancesForProject(project?.name)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }

    Map plots(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }

        Map<PlotType, List<Integer>> plotNumber = [:]

        if (aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_EXTRA)) {
            int count = aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_EXTRA).size()
            plotNumber.put(PlotType.ACESEQ_EXTRA, count ? (0..count - 1) : [])
        }

        if (aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_ALL)) {
            int count = aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_ALL).size()
            plotNumber.put(PlotType.ACESEQ_ALL, count ? (0..count - 1) : [])
        }

        return [
                bamFilePairAnalysis: cmd.bamFilePairAnalysis,
                plotType           : [
                        PlotType.ACESEQ_WG_COVERAGE,
                        PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR,
                        PlotType.ACESEQ_QC_GC_CORRECTED,
                        PlotType.ACESEQ_GC_CORRECTED,
                ],
                plotNumber         : plotNumber,
                error              : null,
        ]
    }

    Map plotImages(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        List<Path> files = aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)
        if (files.isEmpty()) {
            render status: 404
        } else {
            if (cmd.plotType in [PlotType.ACESEQ_EXTRA, PlotType.ACESEQ_ALL]) {
                render file: files[cmd.index].bytes, contentType: "image/png"
            } else {
                render file: files.first().bytes, contentType: "image/png"
            }
        }
    }
}
