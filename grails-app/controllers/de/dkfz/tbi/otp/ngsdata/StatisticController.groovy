package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.validation.Validateable

class StatisticController {

    StatisticService statisticService

    ProjectGroupService projectGroupService

    ProjectService projectService

    JSON projectCountPerDate(ProjectGroupCommand command) {
        ProjectGroup projectGroup = null
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }

        List data = statisticService.projectDateSortAfterDate(projectGroup)
        render statisticService.projectCountPerDate(data) as JSON
    }

    JSON laneCountPerDate(ProjectGroupCommand command) {
        List<Project> projects = null
        if (command.projectGroupName && command.projectGroupName != "OTP") {
            ProjectGroup projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
            projects = projectService.projectByProjectGroup(projectGroup)
        }

        List data = statisticService.laneCountPerDay(projects)
        render statisticService.dataPerDate(data) as JSON
    }

    JSON gigaBasesPerDay(ProjectGroupCommand command) {
        List<Project> projects = null
        if (command.projectGroupName && command.projectGroupName != "OTP") {
            ProjectGroup projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
            projects = projectService.projectByProjectGroup(projectGroup)
        }

        List data = statisticService.gigaBasesPerDay(projects)
        render statisticService.dataPerDate(data) as JSON
    }


    JSON sampleCountPerSequenceType(ProjectGroupCommand command) {
        ProjectGroup projectGroup
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }
        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List sampleCountBySeqType = statisticService.sampleCountPerSequenceType(projectGroup)

        int filterCount = ((sampleCountBySeqType.collect { it[1] }.sum()) ?: 0) / 100

        sampleCountBySeqType.each {
            if (it[1] < filterCount) {
                return
            }
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        sampleCountBySeqType.each {
            if (it[1] < filterCount) {
                return
            }
            labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %"
        }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size()
        ]
        render dataToRender as JSON
    }

    JSON patientsCountPerSequenceType(ProjectGroupCommand command) {
        ProjectGroup projectGroup
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }
        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List patientsCountBySeqType = statisticService.patientsCountPerSequenceType(projectGroup)

        patientsCountBySeqType.each {
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        patientsCountBySeqType.each {
            labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %"
        }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size()
        ]
        render dataToRender as JSON
    }

    public JSON projectCountPerSequenceType(ProjectGroupCommand command) {
        ProjectGroup projectGroup
        if (command.projectGroupName) {
            projectGroup = projectGroupService.projectGroupByName(command.projectGroupName)
        }
        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List projectCountPerSequenceType = statisticService.projectCountPerSequenceType(projectGroup)

        projectCountPerSequenceType.each {
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        projectCountPerSequenceType.each {
            labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %"
        }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size()
        ]
        render dataToRender as JSON
    }

    JSON sampleTypeCountBySeqType(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<String> labels = []
        List<String> labelsPercentage = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List sampleTypeCount = statisticService.sampleTypeCountBySeqType(project)

        sampleTypeCount.each {
            labels << it[0]
            values << it[1]
            projectSequenceCount += it[1]
        }

        sampleTypeCount.each { labelsPercentage << "${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %" }

        Map dataToRender = [
                labels          : labels,
                labelsPercentage: labelsPercentage,
                data            : values,
                count           : values.size()
        ]
        render dataToRender as JSON
    }

    JSON sampleTypeCountByPatient(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<String> labels = []
        List<Integer> values = []

        statisticService.sampleTypeCountByPatient(project).each {
            labels << it[0]
            values << it[1]
        }

        Map dataToRender = [
                projectId: project?.id,
                project  : project?.name,
                labels   : labels,
                data     : values,
                count    : values.size()
        ]
        render dataToRender as JSON
    }

    JSON laneCountPerDateByProject(ProjectCommand command) {
        Project project = projectService.getProjectByName(command.projectName)
        List data = statisticService.laneCountPerDay([project])
        render statisticService.dataPerDate(data) as JSON
    }
}

@Validateable
class ProjectCommand implements Serializable {

    ProjectService projectService

    String projectName

    static constraints = {
        projectName(nullable: false, validator: { val, ProjectCommand obj ->
            return val && (obj.projectService.getProjectByName(val) != null)
        })
    }
}

@Validateable
class ProjectGroupCommand implements Serializable {

    ProjectGroupService projectGroupService
    String projectGroupName

    static constraints = {
        projectGroupName(nullable: false, validator: { val, ProjectGroupCommand obj ->
            return val && (val == "OTP" || (obj.projectGroupService.projectGroupByName(val) != null))
        })
    }
}
