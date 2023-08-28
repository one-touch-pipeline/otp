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

/**
 * script to trigger alignments for a patient or a project or a ilse.
 * It is Possible to restrict the selection to specific SeqTypes
 *
 * The script only works for the old workflow system. For the new workflow system please use the GUI: https://otp.dkfz.de/otp/triggerAlignment/index
 */

import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

OtrsTicketService otrsTicketService = ctx.otrsTicketService
SeqTrackService seqTrackService = ctx.seqTrackService
SamplePairDeciderService samplePairDeciderService = ctx.samplePairDeciderService

LogThreadLocal.withThreadLog(System.out, {
    SeqTrack.withTransaction {
        Collection<SeqTrack> seqTracks = SeqTrack.withCriteria {
            or {
                'in'('id', [
                        -1,
                ].collect { it.toLong() })
                sample {
                    individual {
                        or {
                            'in'('pid', [
                                    '',
                            ])
                            project {
                                'in'('name', [
                                        '',
                                ])
                            }
                        }
                    }
                }
                ilseSubmission(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                    'in'('ilseNumber', [
                            -1,
                    ])
                }
            }
            'in'('seqType', [
                    // SeqTypeService.rnaPairedSeqType,
                    // SeqTypeService.rnaSingleSeqType,
            ])
        }

        List<SeqType> unsupportedSeqTypes = [
            SeqTypeService.wholeGenomePairedSeqType,
            SeqTypeService.exomePairedSeqType,
            SeqTypeService.chipSeqPairedSeqType,
            SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
        ]

        seqTracks.each {
            assert !unsupportedSeqTypes.contains(it.seqType) : "${it.seqType} is not supported by this script, since it is part of the new workflow system. Please use gui: https://otp.dkfz.de/otp/triggerAlignment/index"        }

        // show seqtracks
        // Header
        println([
                "seqtrack_id",
                "project",
                "pid",
                "sampleType",
                "seqType",
                "laneId",
                "run",
                "ilse",
                "libraryPreparationKit",
                "seqPlatform",
                "seqPlatformGroup_id"
        ].join("\t"))
        // Data
        seqTracks.each {
            println([
                    it.id,
                    it.project.name,
                    it.individual.pid,
                    it.sampleType.name,
                    it.seqType,
                    it.laneId,
                    it.run.name,
                    it.ilseId,
                    it.libraryPreparationKit,
                    it.seqPlatform,
                    it.seqPlatformGroup?.id,
            ].join("\t"))
        }
        println "--> ${seqTracks.size()} seqtracks in ${seqTracks*.sample.unique().size()} samples"

        assert false: "DEBUG: transaction intentionally failed to rollback changes"

        // trigger alignment
        List<MergingWorkPackage> mergingWorkPackages = seqTracks.collectMany {
            seqTrackService.decideAndPrepareForAlignment(it)
        }.unique()

        println "\n----------------------\n"
        println mergingWorkPackages.join('\n')
        println "\n----------------------\n"
        println samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages).join('\n')
        println "\n----------------------\n"

        // reset ticket only for seqTracks which gets aligned.
        List<OtrsTicket> otrsTickets = (mergingWorkPackages ? otrsTicketService.findAllOtrsTickets(mergingWorkPackages*.seqTracks.flatten()) : []) as List<OtrsTicket>
        println "found ${otrsTickets.size()} coresponding tickets: ${otrsTickets*.ticketNumber.sort().join(',')}"
        otrsTickets.each {
            otrsTicketService.resetAlignmentAndAnalysisNotification(it)
        }
        println "reset notification state of tickets"
    }
})
null // suppress output spam
