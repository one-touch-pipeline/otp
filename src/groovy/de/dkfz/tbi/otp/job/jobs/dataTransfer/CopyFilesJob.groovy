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

        List<String> allIds = new ArrayList<String>()

        DataFile.findAllByRun(run).each {DataFile file ->
            if (file.project == null) {
                return
            }
            String from = lsdfFilesService.getFileInitialPath(file)
            String to = lsdfFilesService.getFileFinalPath(file)
            println from + " " + to
            String cpCmd = "cp ${from} ${to}"

            File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
            cmdFile.setText(cpCmd)
            cmdFile.setExecutable(true)
            String cmd = "qsub ${cmdFile.name}"
            File responseFile = pbsService.sendPbsJob(cmd)
            List<String> extractedPbsIds = pbsService.extractPbsIds(responseFile)
            println extractedPbsIds
            extractedPbsIds.each {
                allIds << it
            }
        }
        // sleep instead of watch dog
        java.lang.Process sleepProc = "sleep 600".execute()
        sleepProc.waitFor()
    }
}
