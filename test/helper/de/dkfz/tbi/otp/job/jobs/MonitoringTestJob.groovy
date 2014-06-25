package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.MonitoringJob

abstract class MonitoringTestJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {
}
