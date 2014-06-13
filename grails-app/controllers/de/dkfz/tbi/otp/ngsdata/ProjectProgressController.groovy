package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import de.dkfz.tbi.otp.utils.DataTableCommand

class ProjectProgressDataTableCommand extends DataTableCommand {

    //spring set the year, month, day direct in the date object, so no temporar int var are needed
    Date startDate = new Date()
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

    def projectProgressService

    def progress() {


        [
            startDate: new Date().minus(8),
            endDate: new Date(),
            projects: Project.list()*.name
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
        List data = []
        int n=1
        for (Run run in runs) {
            List line = []
            Set<SampleIdentifier> samples = projectProgressService.getSampleIdentifier(run)

            line << run.id
            line << n++
            line << run.name
            line << run.seqCenter.toString().toLowerCase()
            line << samples.sort{it.sample.project.name + " "  + it.name }.collect {[it.sample.individual.id, it.name]}
            data << line
        }
        return data
    }
}
