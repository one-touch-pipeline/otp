package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import org.springframework.beans.factory.annotation.Autowired

class SendSleepJob extends AbstractJobImpl {

    final int N_JOBS = 5

    public void execute() throws Exception {
        /*
        String listOfPids = ""
        for(int i=0; i<N_JOBS; i++) {
            File cmdFile = File.createTempFile("test-", ".tmp", new File(System.getProperty("user.home")))
            cmdFile.setText("""#! /bin/bash
                                date
                                sleep 600
                             """)
            cmdFile.setExecutable(true)
            // Make executable file a pbs job
            //String cmd = "qsub ${cmdFile.name}"
            //String response = pbsService.sendPbsJob(cmd)
            List<String> extractedPbsIds = pbsService.extractPbsIds(response)
            log.debug extractedPbsIds
            listOfPids += extractedPbsIds.get(0) + ","
        }
        //addOutputParameter("pbsIds", listOfPids)
         */
        addOutputParameter("pbsIds", "1,")
    }
}
