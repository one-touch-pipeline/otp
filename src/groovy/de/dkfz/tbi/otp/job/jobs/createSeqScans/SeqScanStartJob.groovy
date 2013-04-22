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

    /*
     * Use of FixedDalay to avoid the problem of invocation of the method
     * multiple times parallel. With FixedRate the method can be executed by
     * two thread parallel, because the execution time can take more then one
     * second and therefore the next execution is already triggered before the
     * first execution is finished.
     * And if the second thread execute the numberOfRunningProcesses method in
     * the hasOpenSlot method before the first thread has execute the
     * createProcess method, the second thread pass the check, because the slot
     * is still free. Only after createProcess has finished, the slot is not
     * longer free, but at that time the second thread already has pass the
     * check. And because the HQL query return the same SeqTrack, both threads
     * create for the same seqTrack a process.
     * Using of FixedDelay fix that problem, because the next execution is
     * always after the previous execution has finished.
     */
    @Scheduled(fixedDelay=1000l)
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