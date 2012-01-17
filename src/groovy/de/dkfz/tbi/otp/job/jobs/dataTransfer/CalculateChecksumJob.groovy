package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Run


public class CalculateChecksumJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    PbsService pbsService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        String scriptName = buildScript(run)
        String jobId = sendScript(scriptName)
        println "Job ${jobId} submitted to PBS"
        addOutputParameter("pbsIds", jobId)
    }

    /**
     * 
     * @param run
     * @return
     */
    private String buildScript(Run run) {
        String[] directories = lsdfFilesService.getAllPathsForRun(run)
        String text = ""
        for(String directory in directories) {
            text += "cd ${directory};md5sum * > files.md5sum"
        }
        File cmdFile = File.createTempFile("md5sumJob", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(text)
        cmdFile.setExecutable(true)
        return cmdFile.name
    }

    /**
     * 
     * @param scriptName
     * @return
     */
    private String sendScript(String scriptName) {
        String cmd = "qsub ${scriptName}"
        //String cmd = "qsub testJob.sh"
        String response = pbsService.sendPbsJob(cmd)
        List<String> extractedPbsIds = pbsService.extractPbsIds(response)
        return extractedPbsIds.get(0)
    }
}
