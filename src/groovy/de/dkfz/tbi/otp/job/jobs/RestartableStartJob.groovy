package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.StartJob

interface RestartableStartJob extends StartJob {

    Process restart(Process process)
}
