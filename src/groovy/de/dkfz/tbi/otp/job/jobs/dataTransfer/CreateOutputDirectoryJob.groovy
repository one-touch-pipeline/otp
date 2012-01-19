package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    PbsService pbsService

    private String projectName

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        projectName = "PROJECT_NAME" //getParameterValueOrClass("project")
        String[] dirs = lsdfFilesService.getListOfRunDirecotries(run, projectName)
        dirs.each {String line ->
            String exitCode = createDirectory(line)
            println "creating directory finished with exit code " + exitCode
        }
    }

    private String createDirectory(String line) {
        String cmd = "mkdir -p " + line
        return pbsService.sendPbsJob(cmd)
    }
}
