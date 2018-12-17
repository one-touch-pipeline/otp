package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.grails.databinding.BindingFormat

import de.dkfz.tbi.otp.utils.DataTableCommand

class ProjectProgressDataTableCommand extends DataTableCommand {

    @BindingFormat('yyyy-MM-dd')
    Date startDate = new Date()
    @BindingFormat('yyyy-MM-dd')
    Date endDate = new Date()

    String projects

    List<String> getProjectNames() {
        if (projects) {
            return projects.split(",") as List
        } else {
            Project.list()*.name
        }
    }
}

@Secured(['ROLE_OPERATOR'])
class ProjectProgressController {

    ProjectProgressService projectProgressService

    def progress() {


        [
            startDate: new Date().minus(8).format('yyyy-MM-dd'),
            endDate: new Date().format('yyyy-MM-dd'),
            projects: Project.list()*.name,
        ]
    }

    JSON dataTableSource(ProjectProgressDataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List<Project> projects = projectProgressService.getProjectsFromNameList(cmd.projectNames)

        //the end date is increased by one day, since the check consider also the time
        List<Run> runs = projectProgressService.getListOfRuns(projects, cmd.startDate, cmd.endDate.plus(1))
        List data = fillTable(runs)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    private List fillTable(List<Run> runs) {
        return runs.collect { Run run ->
            List line = []
            Set<Sample> samples = projectProgressService.getSamples(run)
            line << run.id
            line << run.name
            line << run.seqCenter.toString().toLowerCase()
            line << samples.sort { it.project.name + " "  + it.displayName }.collect { [it.individual.id, it.displayName] }
            return line
        }
    }
}
