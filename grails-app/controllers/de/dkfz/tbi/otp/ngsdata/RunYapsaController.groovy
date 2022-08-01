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
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaResultsService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DataTableCommand

@Secured('isFullyAuthenticated()')
class RunYapsaController extends AbstractAnalysisController {

    RunYapsaResultsService runYapsaResultsService
    ProjectSelectionService projectSelectionService

    static allowedMethods = [
            dataTableResults: "POST",
    ]

    JSON dataTableResults(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        Project project = projectSelectionService.requestedProject
        List data = runYapsaResultsService.getCallingInstancesForProject(project?.name)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        dataToRender.archived = project.archived

        render dataToRender as JSON
    }
}
