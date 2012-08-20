package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateViewByPidJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        List<DataFile> dataFiles = DataFile.findAllByRunAndProjectIsNotNull(run)
        for(DataFile dataFile in dataFiles) {
            log.debug dataFile.fileName + " " + dataFile.project
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
        Realm realm = configService.getRealmDataManagement(file.project)
        executionService.executeCommand(realm, cmd)
    }
}
