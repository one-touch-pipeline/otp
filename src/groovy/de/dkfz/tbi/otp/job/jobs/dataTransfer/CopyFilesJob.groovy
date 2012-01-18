package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CopyFilesJob extends AbstractJobImpl {

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

    /**
     * 
     * @param file
     * @return
     */
    private String buildScript(DataFile file) {
        String from = lsdfFilesService.getFileInitialPath(file)
        String to = lsdfFilesService.getFileFinalPath(file)
        println from + " " + to
        String cpCmd = "cp ${from} ${to}"

        File cmdFile = File.createTempFile("copyJob", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(cpCmd)
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
