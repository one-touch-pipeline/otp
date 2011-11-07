package de.dkfz.tbi.otp.scheduler

/**
 * Trigger for Scheduler.
 * @see SchedulerService
 */
class SchedulerTriggerJob {
    static triggers = {
        simple name: 'schedulerTrigger', repeatInterval: 1000*60
    }
    def schedulerService

    def execute() {
        schedulerService.schedule()
    }
}
