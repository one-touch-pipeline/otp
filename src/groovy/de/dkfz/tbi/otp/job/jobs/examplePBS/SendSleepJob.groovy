package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.PbsService

class SendSleepJob extends AbstractJobImpl {

    @Autowired
    PbsService pbsService

    public void execute() throws Exception {
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText("""#! /bin/bash
                            date
                            sleep 200
                         """)
        cmdFile.setExecutable(true)
        // Make executable file a pbs job
        String cmd = "qsub ${cmdFile.name}"
        String response = pbsService.sendPbsJob(cmd)
        List<String> extractedPbsIds = pbsService.extractPbsIds(response)
        println extractedPbsIds
        addOutputParameter("pbsIds", extractedPbsIds.get(0))
    }
}
