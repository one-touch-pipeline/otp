package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.validation.Validateable
import java.text.SimpleDateFormat

class ProjectStatisticController {

    ProjectService projectService

    StatisticService statisticService

    SeqTypeService seqTypeService

    ProjectOverviewService projectOverviewService

    Map index() {
        return [projects: projectService.getAllProjects()]
    }

    public JSON seqTypeByProject(ProjectTypeCommand command) {
        Project project = projectService.getProjectByName(command.projectName)
        render statisticService.seqTypeByProject(project) as JSON
    }

    public JSON laneNumberByProject(ProjectTypeCommand command) {
        Project project = projectService.getProjectByName(command.projectName)

        List<Map> datas = []
        List<SeqType> seqTypes = statisticService.seqTypeByProject(project)
        seqTypes.each { Long seqTypeId ->
            SeqType seqType = SeqType.get(seqTypeId)

            List<String> labels = []
            List<Integer> values = []

            statisticService.laneNumberByProject(project, seqType).each {
                labels << "${it[0]} ${it[1]}"
                values << it[2]
            }

            Map dataToRender = [
                projectId: project.id,
                seqTypeId: seqType.id,
                project: project.name,
                seqTypeName: seqType.name,
                seqTypeLibName: seqType.libraryLayout,
                labels: labels,
                data: values,
                count: values.size()
            ]
            datas << dataToRender
        }
        render datas as JSON
    }
}

@Validateable
class ProjectTypeCommand implements Serializable {

    ProjectService projectService

    String projectName

    static constraints = {
        projectName(nullable: false, validator: { val, ProjectTypeCommand obj ->
            return val && (obj.projectService.getProjectByName(val) != null)
        })
    }
}