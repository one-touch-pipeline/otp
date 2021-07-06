/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.fastqc

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@Component("fastqcStartJob")
@Scope("singleton")
@Slf4j
class FastqcStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    SeqTrackService seqTrackService

    @Scheduled(fixedDelay = 10000L)
    @Override
    void execute() {
        SessionUtils.withNewSession {
            int minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority == ProcessingPriority.SUPREMUM) {
                return
            }

            SeqTrack.withTransaction {
                SeqTrack seqTrack = seqTrackService.getSeqTrackReadyForFastqcProcessing(minPriority)
                if (seqTrack) {
                    notificationCreator.setStartedForSeqTracks(seqTrack.containedSeqTracks, OtrsTicket.ProcessingStep.FASTQC)
                    log.debug "Creating fastqc process for seqTrack ${seqTrack}"
                    seqTrackService.markFastqcInProgress(seqTrack)
                    createProcess(seqTrack)
                }
            }
        }
    }

    @Override
    Process restart(Process process) {
        assert process

        SeqTrack seqTrack = (SeqTrack) process.processParameterObject

        SeqTrack.withTransaction {
            SeqTrackService.markFastqcInProgress(seqTrack)
            FastqcProcessedFile.withCriteria {
                dataFile {
                    eq "seqTrack", seqTrack
                }
            }*.delete()
            return createProcess(seqTrack)
        }
    }
}
