package de.dkfz.tbi.otp.ngsdata

class ProjectProgressCommand {
    Date startDate
    List projects

    void validate() {
        if (!projects) {
            projects = Project.list()*.name
        }
    }
}

class ProjectProgressController {

    def progress(ProjectProgressCommand ppc) {
        ppc.validate()
        List<Project> projects = Project.findAllByNameInList(ppc.projects)
        List<String> runNames = getRunNames(ppc.startDate, projects)
        List<Run> runs = Run.findAllByNameInList(runNames, [sort: "seqCenter"])
        List data = fillTable(runs)
        [data: data, startDate: ppc.startDate, projects: ppc.projects]
    }

    private List<String> getRunNames(Date date, List<Project> projects) {
        List<DataFile> files = DataFile.findAllByProjectInListAndDateFileSystemGreaterThan(projects, date)
        List<String> runs = files*.run.name
        return runs
    }

    private List fillTable(List<Run> runs) {
        List data = []
        int n=1
        for(Run run in runs) {
            List line = []
            Set<String> samples = getSamples(run)
            line << run.id
            line << n++
            line << run.seqCenter.toString().toLowerCase()
            line << run.name
            line << samples
            data << line
        }
        return data
    }

    private Set<String> getSamples(Run run) {
        Set<String> samples = []
        SeqTrack.findAllByRun(run).each  {SeqTrack track ->
            Sample sample = track.sample
            samples << SampleIdentifier.findBySample(sample)
        }
        return samples
    }
}
