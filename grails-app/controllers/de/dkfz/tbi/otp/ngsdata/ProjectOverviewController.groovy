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

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DataTableCommand

class ProjectOverviewController {

    ProjectOverviewService projectOverviewService
    CommentService commentService
    ProjectSelectionService projectSelectionService

    Map index() {
        return [:]
    }

    JSON individualCountByProject() {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = [individualCount: projectOverviewService.individualCountByProject(project)]
        render dataToRender as JSON
    }

    JSON dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        Project project = projectSelectionService.requestedProject
        List data = projectOverviewService.overviewProjectQuery(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourcePatientsAndSamplesGBCountPerProject(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.patientsAndSamplesGBCountPerProject(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceSampleTypeNameCountBySample(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.sampleTypeNameCountBySample(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceCenterNameRunId(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.centerNameRunId(project)
        List dataLast = projectOverviewService.centerNameRunIdLastMonth(project)

        Map dataLastMap = [:]
        dataLast.each {
            dataLastMap.put(it[0], it[1])
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = []
        data.each {
            List line = []
            line << it[0]
            line << it[1]
            if (dataLastMap.containsKey(it[0])) {
                line << dataLastMap.get(it[0])
            } else {
                line << "0"
            }
            dataToRender.aaData << line
        }
        render dataToRender as JSON
    }
}
