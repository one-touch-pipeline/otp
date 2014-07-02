package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.plugins.springsecurity.Secured
import de.dkfz.tbi.otp.utils.DataTableCommand

class ProjectProgressDataTableCommand extends DataTableCommand {
    int startDate_day
    int startDate_month
    int startDate_year
    int endDate_day
    int endDate_month
    int endDate_year
    Date startDate = new Date(startDate_year - 1900, startDate_month - 1, startDate_day)
    Date endDate = new Date(endDate_year - 1900, endDate_month - 1, endDate_day + 1)
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

        List<Run> runs = projectProgressService.getListOfRuns(projects, cmd.startDate, cmd.endDate)
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
            line << samples.sort{it.sample.individual.project.name + " "  + it.name }.collect {[it.sample.individual.id, it.name]}
            data << line
        }
        return data
    }
}
