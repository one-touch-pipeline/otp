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
            String jobId = sendScript(scriptName)
            println "Job ${jobId} submitted to PBS"
            pbsIds += jobId + ","
        }
        addOutputParameter("pbsIds", pbsIds)
    }

    private boolean md5sumFileExists(DataFile file) {
        String directory = runDirectory(file)
        String md5FileFullPath = directory + "/run" + file.run.name + "/files.md5sum"
        File md5file = new File(md5FileFullPath)
        if (md5file.canRead() && containsDataFile(file, md5file)) {
            return true
        }
        return false
    }

    private boolean containsDataFile(DataFile file, File md5file) {
        String text = md5file.getText()
        if (text.contains(file.fileName)) {
            return true
        }
        return false
    }

    private String runDirectory(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        String[] directories = lsdfFilesService.getAllPathsForRun(file.run)
        for(String directory in directories) {
            if (path.contains(directory)) {
                return directory
            }
        }
        return null // shall never happen (exception ?)
    }

    private String buildScript(DataFile file) {
        String text = scriptText(file)
        File cmdFile = File.createTempFile("md5sumJob", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(text)
        cmdFile.setExecutable(true)
        return cmdFile.name
    }

    private scriptText(DataFile file) {
        String directory = runDirectory(file)
        String runFullPath = directory + "/run" + file.run.name
        String fullFileName = file.formFileName()
        String text = "cd ${runFullPath};md5sum ${fullFileName} >> files.md5sum"
        return text
    }

    private String sendScript(String scriptName) {
        String cmd = "qsub -l nodes=1:lsdf ${scriptName}"
        //String cmd = "qsub testJob.sh"
        String response = pbsService.sendPbsJob(cmd)
        List<String> extractedPbsIds = pbsService.extractPbsIds(response)
        return extractedPbsIds.get(0)
    }
}
