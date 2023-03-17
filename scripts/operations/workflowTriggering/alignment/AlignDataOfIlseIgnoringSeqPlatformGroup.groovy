/*
 * Copyright 2011-2021 The OTP authors
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
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.PanCanAlignmentDecider
import de.dkfz.tbi.otp.ngsdata.IlseSubmission
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Add all lanes of an ilse to MWP ignoring the seqplatform group
 *
 * The script only works for the old workflow system. For the new workflow system please use the GUI: https://otp.dkfz.de/otp/triggerAlignment/index
 *
 * In case some lanes of an ilse was not aligned because the SeqPlatformGroup are not compatible, this script
 * will add this lanes to the other and trigger the alignment.
 */

//-------------------------------
//input area
/**
 * The ilse number
 */
Integer ilseNumber = 0

//-------------------------------
//work area

OtrsTicketService otrsTicketService = ctx.otrsTicketService

PanCanAlignmentDecider panCanAlignmentDecider = ctx.panCanAlignmentDecider

IlseSubmission.withTransaction {
    IlseSubmission ilseSubmission = CollectionUtils.exactlyOneElement(IlseSubmission.findAllByIlseNumber(ilseNumber))

    List<SeqTrack> seqTracks = SeqTrack.findAllByIlseSubmission(ilseSubmission)
    List<SeqTrack> retriggeredSeqTracks = []
    Set<SeqTrack> retriggeredMergingWorkPackages = [] as Set

    List<SeqType> unsupportedSeqTypes = [
            SeqTypeService.wholeGenomePairedSeqType,
            SeqTypeService.exomePairedSeqType,
            SeqTypeService.chipSeqPairedSeqType,
            SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
    ]

    seqTracks.each {
        assert !unsupportedSeqTypes.contains(it.seqType) : "${it.seqType} is not supported by this script, since it is part of the new workflow system. Please use gui: https://otp.dkfz.de/otp/triggerAlignment/index"
    }

    seqTracks.groupBy {
        [
                it.individual,
                it.sampleType,
                it.seqType,
                it.libraryPreparationKit,
        ]
    }.each { List group, List<SeqTrack> seqTracksPerGroup ->
        MergingWorkPackage mergingWorkPackage = CollectionUtils.atMostOneElement(MergingWorkPackage.withCriteria {
            sample {
                eq('individual', group[0])
                eq('sampleType', group[1])
            }
            eq('seqType', group[2])
            eq('libraryPreparationKit', group[3])
        })
        if (!mergingWorkPackage) {
            SeqTrack seqTrack = seqTracksPerGroup.first()
            mergingWorkPackage = panCanAlignmentDecider.decideAndPrepareForAlignment(seqTrack, false).first()
            retriggeredSeqTracks << seqTrack
            seqTracksPerGroup.remove(seqTrack)
            retriggeredMergingWorkPackages << mergingWorkPackage
        }
        seqTracksPerGroup.each {
            if (mergingWorkPackage.seqTracks.add(it)) {
                mergingWorkPackage.needsProcessing = true
                retriggeredSeqTracks << it
                retriggeredMergingWorkPackages << mergingWorkPackage
            }
        }
        mergingWorkPackage.save(flush: true)

        println "Trigger ${mergingWorkPackage}: ${mergingWorkPackage.needsProcessing}"
    }
    println "\nadded ${retriggeredSeqTracks.size()} lanes to ${retriggeredMergingWorkPackages.size()} mergingWorkPackages\n"

    if (retriggeredSeqTracks) {
        otrsTicketService.findAllOtrsTickets(retriggeredSeqTracks).each {
            otrsTicketService.resetAlignmentAndAnalysisNotification(it)
            println "reset notification for ticket ${it}"
        }
    }
    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}

''
