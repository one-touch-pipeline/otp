package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.*

interface RestartableStartJob extends StartJob {

    Process restart(Process process)
}
