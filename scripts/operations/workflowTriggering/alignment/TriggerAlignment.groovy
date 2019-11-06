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

/**
 * script to trigger alignments for a patient or a project or a ilse.
 * It is Possible to restrict the selection to specific SeqTypes
 */

import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

LogThreadLocal.withThreadLog(System.out, {
    SeqTrack.withTransaction {

        Collection<SeqTrack> seqTracks = SeqTrack.withCriteria {
            or {
                sample {
                    individual {
                        or {
                            'in'('mockPid', [
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
                ilseSubmission(JoinType.LEFT_OUTER_JOIN.getJoinTypeValue()) {
                    'in'('ilseNumber', [
                            -1,
                    ])
                }
            }
            'in'('seqType', [
                    //SeqTypeService.wholeGenomePairedSeqType,
                    //SeqTypeService.exomePairedSeqType,
                    //SeqTypeService.wholeGenomeBisulfitePairedSeqType,
                    //SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
                    //SeqTypeService.rnaPairedSeqType,
                    //SeqTypeService.rnaSingleSeqType,
                    //SeqTypeService.chipSeqPairedSeqType,
            ])
        }

        //show seqtracks
        // Header
        println([
                "seqtrack_id",
                "project",
                "mockPid",
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
                    it.individual.mockPid,
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

        /*
        //trigger alignment
        List<AbstractMergingWorkPackage> mergingWorkPackages = seqTracks.collectMany {
            ctx.seqTrackService.decideAndPrepareForAlignment(it)
        }.unique()

        println "\n----------------------\n"
        println mergingWorkPackages.join('\n')
        println "\n----------------------\n"
        println ctx.samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages).join('\n')
        //*/

    }
})
null // suppress output spam
