package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.*

public interface RestartableStartJob extends StartJob {
    public void restart()
}
