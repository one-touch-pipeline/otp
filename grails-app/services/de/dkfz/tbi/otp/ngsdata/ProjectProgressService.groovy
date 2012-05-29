package de.dkfz.tbi.otp.ngsdata

class ProjectProgressService {

    List<Run> getListOfRuns(List<Project> projects, Date date) {
        List<DataFile> files = DataFile.findAllByProjectInListAndDateFileSystemGreaterThan(projects, date)
        List<String> runNames = files*.run.name
        List<Run> runs = Run.findAllByNameInList(runNames, [sort: "seqCenter"])
    }

   Set<String> getSamples(Run run) {
        Set<String> samples = []
        SeqTrack.findAllByRun(run).each {SeqTrack track ->
            Sample sample = track.sample
            samples << SampleIdentifier.findBySample(sample)
        }
        return samples
    }
}
