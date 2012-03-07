package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    PbsService pbsService

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        Set<String> projects = projects(run)
        projects.each {String projectName ->
            String[] dirs = lsdfFilesService.getListOfRunDirecotries(run, projectName)
            dirs.each {String line ->
                String exitCode = createDirectory(line)
                println "creating directory finished with exit code " + exitCode
            }
        }
    }

    private String createDirectory(String line) {
        String cmd = "mkdir -p " + line
        return pbsService.sendPbsJob(cmd)
    }

    private Set<String> projects(Run run) {
        Set<String> projects = new HashSet<String>()
        List<DataFile> files = DataFile.findAllByRun(run)
        for(DataFile file in files) {
            if (file.project) {
                projects << file.project.name
            }
        }
        return projects
    }
}
