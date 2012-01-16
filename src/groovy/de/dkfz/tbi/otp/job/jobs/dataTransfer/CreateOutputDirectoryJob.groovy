package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    private String projectName

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        println "run name = " + run
        projectName = getParameterValueOrClass("project")
        String[] dirs = lsdfFilesService.getListOfRunDirecotries(run, projectName)

        dirs.each {String line ->
            int exitCode = createDirectory(line)
            println "creating directory finished with exit code" + exitCode
        }
    }

    private int createDirectory(String line) {
        String cmd = "mkdir -p " + line
        java.lang.Process process = cmd.execute()
        process.waitFor()
        return process.exitValue()
    }
}
