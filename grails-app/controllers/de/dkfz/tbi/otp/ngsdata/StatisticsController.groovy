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

import org.springframework.security.access.annotation.Secured

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.TimeFormats

import java.text.SimpleDateFormat

@Secured("hasRole('ROLE_OPERATOR')")
class StatisticsController {
    ProjectService projectService
    UserProjectRoleService userProjectRoleService
    ClusterJobService clusterJobService
    SampleService sampleService
    ProjectSelectionService projectSelectionService
    ProjectGroupService projectGroupService

    static allowedMethods = [
            downloadDirectoriesCSV: "GET",
            kpi                   : "GET",
    ]

    def downloadDirectoriesCSV() {
        StringBuilder output = new StringBuilder()

        projectService.allProjects.each { Project project ->
            output << projectService.getProjectDirectory(project) << ","
            output << project.dirAnalysis ?: ''
            output << "\n"
        }

        render(file: new ByteArrayInputStream(output.toString().getBytes("UTF-8")), fileName: 'directories.csv', contentType: 'text/csv')
    }

    def kpi() {
        List<Project> projects
        if (params.projectGroup) {
            projects = projectService.allProjects.intersect(Project.findAllByProjectGroup(projectGroupService.projectGroupByName(params.projectGroup)))
        } else {
            projects = projectService.allProjects
        }
        Project project = projectSelectionService.selectedProject

        List<ProjectGroup> availableProjectGroups = projects.groupBy { it.projectGroup }.keySet().findAll().toList()
                .sort { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name) }

        SimpleDateFormat dateFormat = new SimpleDateFormat(TimeFormats.DATE.format, Locale.ENGLISH)
        Date startDate = params.start && params.end ? dateFormat.parse(params.start) : null
        Date endDate = params.end && params.end ? dateFormat.parse(params.end) : new Date()

        if (!projects) {
            return [
                    numberOfProject: 0,
            ]
        }
        int samples = sampleService.getCountOfSamplesForSpecifiedPeriodAndProjects(startDate, endDate, projects)

        int numberOfProjects = projectService.getCountOfProjectsForSpecifiedPeriod(startDate, endDate, projects)

        int clusterJobs = clusterJobService.getNumberOfClusterJobsForSpecifiedPeriodAndProjects(startDate, endDate, projects)

        int usersCreated = (startDate && endDate) ? userProjectRoleService.getNumberOfValidUsersForProjects(projects, startDate, endDate) : 0

        int users = userProjectRoleService.getNumberOfValidUsersForProjects(projects)

        int samplesProject = sampleService.getCountOfSamplesForSpecifiedPeriodAndProjects(startDate, endDate, [project])

        int clusterJobsProject = clusterJobService.getNumberOfClusterJobsForSpecifiedPeriodAndProjects(startDate, endDate, [project])

        int usersCreatedProject = (startDate && endDate) ? userProjectRoleService.getNumberOfValidUsersForProjects([project], startDate, endDate) : 0

        int usersProject = userProjectRoleService.getNumberOfValidUsersForProjects([project])

        return [
                projectGroup               : params.projectGroup,
                availableProjectGroups     : availableProjectGroups,
                numberOfProject            : numberOfProjects,
                numberOfSamples            : samples,
                numberOfUsers              : users,
                numberOfCreatedUsers       : usersCreated,
                numberOfClusterJobs        : clusterJobs,
                numberOfSamplesProject     : samplesProject,
                numberOfUsersProject       : usersProject,
                numberOfCreatedUsersProject: usersCreatedProject,
                numberOfClusterJobsProject : clusterJobsProject,
                startDate                  : startDate ? dateFormat.format(startDate) : "",
                endDate                    : endDate ? dateFormat.format(endDate) : "",
                maxDate                    : dateFormat.format(new Date()),
        ]
    }
}
