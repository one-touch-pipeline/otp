package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.PbsService

class MyPBSWatchdogJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    PbsService pbsService

    public void execute() throws Exception {
        String jobIds = getParameterValueOrClass("pbsIds")
        List<String> listJobIds = new ArrayList<String>()
        listJobIds.add(jobIds)

        boolean finished = false
        while(!finished) {
            finished = true
            Map<String, Boolean> validatedIds = pbsService.validate(listJobIds)
            validatedIds.each {String job, boolean isRunning ->
                println "${job} ${isRunning}"
                if (isRunning) {
                    finished = false
                }
            }
            sleep(30)
        }
        succeed()
    }

    private sleep(int time) {
        java.lang.Process process = "sleep ${time}".execute()
        process.waitFor()
    }
}
