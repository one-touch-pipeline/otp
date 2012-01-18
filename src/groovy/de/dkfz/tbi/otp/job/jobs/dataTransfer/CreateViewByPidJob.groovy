package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateViewByPidJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    private String projectName

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        projectName = "PROJECT_NAME" //getParameterValueOrClass("project")

        List<DataFile> dataFiles = DataFile.findAllByRun(run)
        for(DataFile dataFile in dataFiles) {
            println dataFile.name + " " + dataFile.project
            if (dataFile.project.toString() != projectName) {
                continue
            }
            linkDataFile(file)
        }
    }

    private linkDataFile(DataFile file) {
        String target = lsdfFilesService.getFileFinalPath(dataFile)
        String linkName = lsdfFilesService.getFileViewByPidPath(dataFile)
        if (linkName == null || target == null) {
            return
        }
        String dirName = linkName.substring(0, linkName.lastIndexOf('/'))
        String cmd = "mkdir -p ${dirName}"
        executeCommand(cmd)

        cmd = "ln -s " + target + " " + linkName
        executeCommand(cmd)
    }

    // TODO move to service and store command in a database
    private executeCommand(String cmd) {
        println cmd
        java.lang.Process process = cmd.execute()
        process.waitFor()
    }
}
