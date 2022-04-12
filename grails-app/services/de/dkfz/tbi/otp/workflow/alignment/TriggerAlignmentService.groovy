/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.withdraw.RoddyBamFileWithdrawService
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

@SuppressWarnings('UseCollectMany')
@Transactional(readOnly = true)
class TriggerAlignmentService {

    SeqTrackService seqTrackService
    SamplePairDeciderService samplePairDeciderService
    OtrsTicketService otrsTicketService
    AllDecider allDecider
    RoddyBamFileWithdrawService roddyBamFileWithdrawService

    @Transactional
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Collection<MergingWorkPackage> triggerAlignment(Collection<SeqTrack> seqTracks, boolean withdrawBamFiles = false) {
        // Mark the bam files as withdrawn
        if (withdrawBamFiles) {
            roddyBamFileWithdrawService.collectObjects(seqTracks as List<SeqTrack>).each { RoddyBamFile bamFile ->
                bamFile.withdraw()
                bamFile.save(flush: true)
            }
        }

        // Modify the notification status
        otrsTicketService.findAllOtrsTickets(seqTracks).each {
            otrsTicketService.resetAlignmentAndAnalysisNotification(it)
        }

        // Start alignment workflows
        Collection<SeqTrack> seqTracksInNewWorkflowSystem = allDecider.findAllSeqTracksInNewWorkflowSystem(seqTracks)
        Collection<MergingWorkPackage> mergingWorkPackages =  allDecider.decide(seqTracksInNewWorkflowSystem*.workflowArtefact).findAll {
            it.artefactType == ArtefactType.BAM
        }*.artefact*.get()*.workPackage + (seqTracks - seqTracksInNewWorkflowSystem).collectMany {
            seqTrackService.decideAndPrepareForAlignment(it)
        }.unique()

        if (mergingWorkPackages) {
            samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
        }

        return mergingWorkPackages
    }
}
