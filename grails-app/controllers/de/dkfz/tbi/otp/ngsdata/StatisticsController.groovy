package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.infrastructure.*
import grails.converters.*
import grails.orm.*
import grails.plugin.springsecurity.*

class StatisticsController {
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    ProjectOverviewService projectOverviewService

    def downloadDirectoriesCSV() {
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        StringBuilder output

        selection.projects.each { Project project ->
            output << project.projectDirectory << ","
            output << project.dirAnalysis ?: ''
            output << "\n"
        }

        render file: new ByteArrayInputStream(output.toString().getBytes("UTF-8")), fileName:'directories.csv', contentType: 'text/csv'
    }


    def kpi() {
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        HibernateCriteriaBuilder c

        c = Sample.createCriteria()
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

        return [
                numberOfProject    : selection.projects.size(),
                numberOfSamples    : samples,
                numberOfUsers      : projectOverviewService.getAccessPersons(selection.projects).size(),
                numberOfClusterJobs: clusterJobs,
        ]
    }
}
