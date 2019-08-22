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
package de.dkfz.tbi.otp.job.jobs.dataInstallation

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.SessionUtils

@Component("dataInstallationStartJob")
@Scope("singleton")
@Slf4j
class DataInstallationStartJob extends AbstractStartJobImpl {

    @Autowired
    SeqTrackService seqTrackService

    @Scheduled(fixedDelay=5000L)
    @Override
    void execute() {
        SessionUtils.withNewSession {
            ProcessingPriority minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority.priority > ProcessingPriority.MAXIMUM.priority) {
                return
            }

            SeqTrack seqTrack = seqTrackService.seqTrackReadyToInstall(minPriority)
            if (seqTrack) {
                SeqTrack.withTransaction {
                    notificationCreator.setStartedForSeqTracks([seqTrack], OtrsTicket.ProcessingStep.INSTALLATION)
                    seqTrack.dataInstallationState = SeqTrack.DataProcessingState.IN_PROGRESS
                    assert seqTrack.save(flush: true)
                    createProcess(seqTrack)
                    log.debug "Installing SeqTrack ${seqTrack} of run ${seqTrack.run.name}"
                }
            }
        }
    }
}
