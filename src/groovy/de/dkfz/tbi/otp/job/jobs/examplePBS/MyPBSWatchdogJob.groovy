package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.ExecutionService

class MyPBSWatchdogJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ExecutionService executionService

    final int timeout = 10

    public void execute() throws Exception {
        String jobIds = getParameterValueOrClass("pbsIds")
        List<String> listJobIds = parseInputString(jobIds)

        while(!checkIfAllFinished(listJobIds)) {
            sleep(timeout)
        }
        succeed()
    }

    private boolean checkIfAllFinished(List<String> listJobIds) {
        boolean finished = true
        Map<String, Boolean> validatedIds = executionService.validate(listJobIds)
        validatedIds.each {String job, boolean isRunning ->
            println "${job} ${isRunning}"
            if (isRunning) {
                finished = false
            }
        }
        return finished
    }

    private sleep(int time) {
        java.lang.Process process = "sleep ${time}".execute()
        process.waitFor()
    }

    private parseInputString(String jobIds) {
        List<String> pbsIds = jobIds.tokenize(",")
    }
}
