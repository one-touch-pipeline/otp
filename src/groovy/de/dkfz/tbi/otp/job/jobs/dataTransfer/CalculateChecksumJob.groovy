package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.DataFile


public class CalculateChecksumJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    PbsService pbsService

    String suffix = ".md5sum"

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        String pbsIds = "1,"
        DataFile.findAllByRunAndProjectIsNotNull(run).each {DataFile file ->
            if (md5sumFileExists(file)) {
                println "checksum file already exists ..."
                return
            }
            String scriptName = buildScript(file)
            String cmd = scriptText(file)
            String jobId = sendScript(cmd)
            println "Job ${jobId} submitted to PBS"
            pbsIds += jobId + ","
        }
        addOutputParameter("pbsIds", pbsIds)
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

    private String buildScript(DataFile file) {
        String text = scriptText(file)
        File cmdFile = File.createTempFile("md5sumJob", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(text)
        cmdFile.setExecutable(true)
        return cmdFile.name
    }

    private scriptText(DataFile file) {
        String directory = dirToMd5File(file)
        String fileName = file.fileName
        String md5FileName = fileName + suffix
        String text = "cd ${directory};md5sum ${fileName} > ${md5FileName};chmod 440 ${md5FileName}"
        return text
    }

    private String sendScript(String text) {
        String cmd = "echo '${text}' | qsub -l nodes=1:lsdf"
        println cmd
        //String cmd = "qsub testJob.sh"
        String response = pbsService.sendPbsJob(cmd)
        List<String> extractedPbsIds = pbsService.extractPbsIds(response)
        return extractedPbsIds.get(0)
    }
}
