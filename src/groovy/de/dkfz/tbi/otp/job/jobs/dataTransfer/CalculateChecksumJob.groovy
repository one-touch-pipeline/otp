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

        String pbsIds = ""
        DataFile.findAllByRun(run).each {DataFile file ->
            if (file.project == null) {
                return
            }
            String scriptName = buildScript(file)
            String jobId = sendScript(scriptName)
            println "Job ${jobId} submitted to PBS"
            pbsIds += jobId + ","
        }
        addOutputParameter("pbsIds", pbsIds)
    }

    private String buildScript(DataFile file) {
        String text = scriptText(file)
        File cmdFile = File.createTempFile("md5sumJob", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(text)
        cmdFile.setExecutable(true)
        return cmdFile.name
    }

    private scriptText(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        String[] directories = lsdfFilesService.getAllPathsForRun(file.run)
        String text = ""
        for(String directory in directories) {
            if (path.contains(directory)) {
                String runFullPath = directory + "/run" + file.run.name
                String fullFileName = file.formFileName()
                text = "cd ${runFullPath};md5sum ${fullFileName} >> files.md5sum"
            }
        }
        return text
    }

    private String sendScript(String scriptName) {
        String cmd = "qsub ${scriptName}"
        //String cmd = "qsub testJob.sh"
        String response = pbsService.sendPbsJob(cmd)
        List<String> extractedPbsIds = pbsService.extractPbsIds(response)
        return extractedPbsIds.get(0)
    }
}
