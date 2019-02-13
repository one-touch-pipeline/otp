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
