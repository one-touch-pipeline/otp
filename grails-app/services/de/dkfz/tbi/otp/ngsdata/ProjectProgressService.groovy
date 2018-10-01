package de.dkfz.tbi.otp.ngsdata

class ProjectProgressService {

    List<Project> getProjectsFromNameList(List<String> projectNames) {
        return Project.findAllByNameInList(projectNames)
    }

    List<Run> getListOfRuns(List<Project> projects, Date startDate, Date endDate) {
        List<DataFile> files = DataFile.findAllByProjectInListAndDateFileSystemBetween(projects, startDate, endDate)
        return files*.seqTrack*.run.sort { it.seqCenter.name }.unique()
    }

    Set<Sample> getSamples(Run run) {
        SeqTrack.findAllByRun(run)*.sample.unique()
    }
}
