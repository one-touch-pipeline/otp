/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.jobs.createSeqScans

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.SessionUtils

@Component("seqScanStartJob")
@Scope("singleton")
@Slf4j
class SeqScanStartJob extends AbstractStartJobImpl  {


    static final int MAX_RUNNING = 1
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
    @Scheduled(fixedDelay=30000L)
    @Override
    void execute() {
        SessionUtils.withNewSession {
            if (!hasOpenSlots()) {
                return
            }
            SeqTrack seqTrack = SeqTrack.find(hql)
            if (seqTrack == null) {
                return
            }
            createProcess(seqTrack)
            log.info("${name}: job started for seqTrack ${seqTrack}")
        }
    }

    boolean hasOpenSlots() {
        if (!getJobExecutionPlan() || !getJobExecutionPlan().enabled) {
            return false
        }
        int numberOfRunning = numberOfRunningProcesses()
        return numberOfRunning < MAX_RUNNING
    }

   /**
     * returns number of running processes for this execution plan
     */
    private int numberOfRunningProcesses() {
        return Process.countByFinishedAndJobExecutionPlan(false, getJobExecutionPlan())
    }
}
