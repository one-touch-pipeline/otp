package de.dkfz.tbi.otp.ngsdata

class ProjectProgressService {

    List<Project> getProjectsFromNameList(List<String> projectNames) {
        return Project.findAllByNameInList(projectNames)
    }

    List<Run> getListOfRuns(List<Project> projects, Date startDate, Date endDate) {
        List<DataFile> files = DataFile.findAllByProjectInListAndDateFileSystemBetween(projects, startDate, endDate)
        List<String> runNames = files*.run.name
        List<Run> runs = Run.findAllByNameInList(runNames, [sort: "seqCenter"])
        return runs
    }

    Set<SampleIdentifier> getSampleIdentifier(Run run) {
        Set<SampleIdentifier> sampleIdentifiers = []
        SeqTrack.findAllByRun(run).each { SeqTrack track ->
            Sample sample = track.sample
            sampleIdentifiers << SampleIdentifier.findBySample(sample)
        }
        return sampleIdentifiers
    }
}
