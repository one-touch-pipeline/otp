package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Realm


public class CalculateChecksumJob extends AbstractJobImpl {

    final String paramName = "__pbsIds"

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    String suffix = ".md5sum"

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        List<DataFile> files =  DataFile.findAllByRunAndProjectIsNotNull(run)

        String pbsIds = "1,"
        files.each {DataFile file ->
            if (md5sumFileExists(file)) {
                println "checksum file already exists"
                return
            }
            String cmd = scriptText(file)
            Realm realm = configService.getRealmDataManagement(file.project)
            String jobId = sendScript(realm, cmd)
            println "Job ${jobId} submitted to PBS"
            pbsIds += jobId + ","
        }
        addOutputParameter(paramName, pbsIds)
    }

    private String dirToMd5File(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        String dirPath = path.substring(0, path.lastIndexOf("/")+1)
        return dirPath
    }

    private String pathToMd5File(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        String md5file = path + suffix
        return md5file
    }

    private boolean md5sumFileExists(DataFile file) {
        String path = pathToMd5File(file)
        File md5file = new File(path)
        if (md5file.canRead()) {
            return true
        }
        return false
    }

    private scriptText(DataFile file) {
        String directory = dirToMd5File(file)
        String fileName = file.fileName
        String md5FileName = fileName + suffix
        String text = "cd ${directory};md5sum ${fileName} > ${md5FileName};chmod 440 ${md5FileName}"
        return text
    }

    private String sendScript(Realm realm, String text) {
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            println "Number of PBS is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
