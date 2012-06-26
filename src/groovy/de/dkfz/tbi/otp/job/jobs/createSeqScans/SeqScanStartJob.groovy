package de.dkfz.tbi.otp.job.jobs.createSeqScans

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
@Component("seqScanStartJob")
@Scope("singleton")
class SeqScanStartJob extends AbstractStartJobImpl  {

    @Autowired
    RunProcessingService runProcessingService

    final int MAX_RUNNING = 1
    final String name = "seqScanWorkflow"
    final String hql = "FROM SeqTrack as track WHERE track.id not in (SELECT seqTrack.id from MergingAssignment)"

    @Scheduled(fixedRate=1000l)
    void execute() {
        if (!hasOpenSlots()) {
            return
        }
        if (!runProcessingService.isMetaDataProcessingFinished()) {
            return
        }
        SeqTrack seqTrack = SeqTrack.find(hql)
        if (seqTrack == null) {
            return
        }
        createProcess(new ProcessParameter(value: seqTrack.id.toString(), className: seqTrack.class.name))
        println "${name}: job started for seqTrack ${seqTrack}"
    }

    boolean hasOpenSlots() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            return false
        }
        int numberOfRunning = numberOfRunningProcesses()
        if (numberOfRunning >= MAX_RUNNING) {
            return false
        }
        return true
    }

   /**
     * returns number of running processes for this execution plan
     * @return
     */
    private int numberOfRunningProcesses() {
        return Process.countByFinishedAndJobExecutionPlan(false, getExecutionPlan())
    }
}