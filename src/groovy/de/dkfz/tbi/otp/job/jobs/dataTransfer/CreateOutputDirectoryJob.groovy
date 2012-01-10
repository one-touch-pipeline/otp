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

        //String cmd = ""
        dirs.each {String line ->
            String cmd = "mkdir -p " + line
            println cmd
            java.lang.Process process = cmd.execute()
            process.waitFor()
            println "creating directory finished with " + process.exitValue()
        }
        //println cmd
    }
}
