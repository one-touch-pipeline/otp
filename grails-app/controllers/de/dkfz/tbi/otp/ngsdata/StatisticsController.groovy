package de.dkfz.tbi.otp.ngsdata

import grails.orm.HibernateCriteriaBuilder

import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.infrastructure.ClusterJob

class StatisticsController {
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    UserProjectRoleService userProjectRoleService

    def downloadDirectoriesCSV() {
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        StringBuilder output = new StringBuilder()

        selection.projects.each { Project project ->
            output << project.projectDirectory << ","
            output << project.dirAnalysis ?: ''
            output << "\n"
        }

        render file: new ByteArrayInputStream(output.toString().getBytes("UTF-8")), fileName:'directories.csv', contentType: 'text/csv'
    }

    def kpi() {
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        if (!selection.projects) {
            return [
                    numberOfProject : 0,
            ]
        }

        HibernateCriteriaBuilder c = Sample.createCriteria()
        int samples = c.count {
            individual {
                'in'('project', selection.projects)
            }
        }

        c = ClusterJob.createCriteria()
        int clusterJobs = c.count {
            individual {
                'in'('project', selection.projects)
            }
        }

        int users = userProjectRoleService.getNumberOfValidUsersForProjects(selection.projects)

        return [
                numberOfProject    : selection.projects.size(),
                numberOfSamples    : samples,
                numberOfUsers      : users,
                numberOfClusterJobs: clusterJobs,
        ]
    }
}
