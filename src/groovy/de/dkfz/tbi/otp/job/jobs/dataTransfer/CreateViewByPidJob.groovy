package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateViewByPidJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    PbsService pbsService

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        List<DataFile> dataFiles = DataFile.findAllByRunAndProjectIsNotNull(run)
        for(DataFile dataFile in dataFiles) {
            println dataFile.fileName + " " + dataFile.project
            linkDataFile(dataFile)
        }
    }

    private linkDataFile(DataFile file) {
        String target = lsdfFilesService.getFileFinalPath(file)
        String linkName = lsdfFilesService.getFileViewByPidPath(file)
        if (linkName == null || target == null) {
            return
        }
        String dirName = linkName.substring(0, linkName.lastIndexOf('/'))
        String cmd = "mkdir -p ${dirName};"
        cmd += "ln -s " + target + " " + linkName
        executeCommand(cmd)
    }

    // TODO move to service and store command in a database
    private executeCommand(String cmd) {
        println cmd
        String response = pbsService.sendPbsJob(cmd)
        println response
        //java.lang.Process process = cmd.execute()
        //process.waitFor()
    }
}
