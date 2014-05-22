package de.dkfz.tbi.otp.ngsdata

import grails.plugins.springsecurity.Secured

class ProjectProgressCommand {
    Date startDate
    List projects

    void validate() {
        if (!projects) {
            projects = Project.list()*.name
        }
    }
}

@Secured(['ROLE_OPERATOR'])
class ProjectProgressController {

    def projectProgressService

    def progress(ProjectProgressCommand ppc) {
        ppc.validate()
        List<Project> projects = projectProgressService.getProjectsFromNameList(ppc.projects)
        List<Run> runs = projectProgressService.getListOfRuns(projects, ppc.startDate)
        List data = fillTable(runs)
        [data: data, startDate: ppc.startDate, projects: ppc.projects]
    }

    private List fillTable(List<Run> runs) {
        List data = []
        int n=1
        for (Run run in runs) {
            List line = []
            Set<String> samples = projectProgressService.getSampleIdentifier(run)
            line << run.id
            line << n++
            line << run.seqCenter.toString().toLowerCase()
            line << run.name
            line << samples
            data << line
        }
        return data
    }
}
