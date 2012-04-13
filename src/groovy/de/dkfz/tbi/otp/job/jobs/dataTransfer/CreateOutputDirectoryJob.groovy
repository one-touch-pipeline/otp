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
                def exitCode = createDirectory(line)
                println "creating directory finished with exit code " + exitCode.toString()
            }
        }
    }

    private def createDirectory(String line) {
        String cmd = "mkdir -p " + line
        println cmd
        def out = pbsService.sendPbsJob(cmd)
        return out
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
