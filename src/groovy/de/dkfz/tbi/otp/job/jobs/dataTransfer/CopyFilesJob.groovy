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

        String pbsIds = "1,"
        DataFile.findAllByRunAndProjectIsNotNull(run).each {DataFile file ->
            String scriptName = buildScript(file)
            String jobId = sendScript(scriptName)
            println "Job ${jobId} submitted to PBS"
            pbsIds += jobId + ","
        }
        addOutputParameter("pbsIds", pbsIds)
    }

    private String buildScript(DataFile file) {
        String cpCmd = scriptText(file)
        File cmdFile = File.createTempFile("copyJob", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(cpCmd)
        cmdFile.setExecutable(true)
        return cmdFile.name
    }

    private String scriptText(DataFile file) {
        String from = lsdfFilesService.getFileInitialPath(file)
        String to = lsdfFilesService.getFileFinalPath(file)
        println from + " " + to
        String cmd = "cp ${from} ${to};chmod 440 ${to}"
        return cmd
    }

    private String sendScript(String scriptName) {
        String cmd = "qsub -l nodes=1:lsdf ${scriptName}"
        //String cmd = "qsub testJob.sh"
        String response = pbsService.sendPbsJob(cmd)
        List<String> extractedPbsIds = pbsService.extractPbsIds(response)
        return extractedPbsIds.get(0)
    }
}
