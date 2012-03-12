package de.dkfz.tbi.otp.job.jobs.utils

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process

class ThrottlingStartJob extends AbstractStartJobImpl {

    void execute() {
        
    }

    boolean hasOpenSlots(int maxJobs) {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            return false
        }
        int numberOfRunning = numberOfRunningProcesses()
        if (numberOfRunning >= maxJobs) {
            return false
        }
    }

   /**
     * returns number of running processes for this execution plan
     * @return
     */
    private int numberOfRunningProcesses() {
        return Process.countByFinishedAndJobExecutionPlan(false, getExecutionPlan())
    }
}
