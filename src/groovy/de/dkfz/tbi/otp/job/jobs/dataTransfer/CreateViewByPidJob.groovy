package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateViewByPidJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    @Autowired
    RunProcessingService runProcessingService

    @Override
    public void execute() throws Exception {
        Run run = Run.get(Long.parseLong(getProcessParameterValue()))
        List<DataFile> files = runProcessingService.dataFilesForProcessing(run)
        for (DataFile dataFile in files) {
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
        String cmd = "umask 027; mkdir -p -m 2750 ${dirName};"
        cmd += "ln -s " + target + " " + linkName
        Realm realm = configService.getRealmDataManagement(file.project)
        executionService.executeCommand(realm, cmd)
    }
}
