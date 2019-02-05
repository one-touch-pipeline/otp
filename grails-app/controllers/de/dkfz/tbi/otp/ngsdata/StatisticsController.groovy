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

import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import java.text.SimpleDateFormat

class StatisticsController {
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    UserProjectRoleService userProjectRoleService
    ClusterJobService clusterJobService
    SampleService sampleService

    static allowedMethods = [
            downloadDirectoriesCSV: "GET",
            kpi                   : ["GET", "POST"],
    ]

    def downloadDirectoriesCSV() {
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        StringBuilder output = new StringBuilder()

        selection.projects.each { Project project ->
            output << project.projectDirectory << ","
            output << project.dirAnalysis ?: ''
            output << "\n"
        }

        render file: new ByteArrayInputStream(output.toString().getBytes("UTF-8")), fileName: 'directories.csv', contentType: 'text/csv'
    }

    def kpi() {
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        Date startDate = params.datepickerStartDate ? new SimpleDateFormat("yyyy-MM-dd").parse(params.datepickerStartDate) : null
        Date endDate = params.datepickerEndDate ? new SimpleDateFormat("yyyy-MM-dd").parse(params.datepickerEndDate) : null

        if (!selection.projects) {
            return [
                    numberOfProject: 0,
            ]
        }
        int samples = sampleService.getCountOfSamplesForSpecifiedPeriodAndProjects(startDate, endDate, selection.projects)

        int projects = projectService.getCountOfProjectsForSpecifiedPeriod(startDate, endDate, selection.projects)

        int clusterJobs = clusterJobService.getNumberOfClusterJobsForSpecifiedPeriodAndProjects(startDate, endDate, selection.projects)

        int usersCreated = (startDate && endDate) ? userProjectRoleService.getNumberOfValidUsersForProjects(selection.projects, startDate, endDate) : 0

        int users = userProjectRoleService.getNumberOfValidUsersForProjects(selection.projects)

        return [
                numberOfProject     : projects,
                numberOfSamples     : samples,
                numberOfUsers       : users,
                numberOfCreatedUsers: usersCreated,
                numberOfClusterJobs : clusterJobs,
                startDate           : params.datepickerStartDate,
                endDate             : params.datepickerEndDate,
        ]
    }
}
