


/**
 * script to trigger alignments for a patient or a project or a ilse.
 * It is Possible to restrict the selection to specific SeqTypes
 *
 * The align flag of all used runsegments of the seqtracks are set to true.
 */

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import org.hibernate.sql.*

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
                    SeqTypeService.wholeGenomePairedSeqType,
                    SeqTypeService.exomePairedSeqType,
                    SeqTypeService.wholeGenomeBisulfitePairedSeqType,
                    SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
                    SeqTypeService.rnaPairedSeqType,
                    SeqTypeService.chipSeqPairedSeqType,
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
        //make all used run segments alignable
        DataFile.findAllBySeqTrackInList(seqTracks)*.runSegment.unique()*.align = true
        //trigger alignment
        seqTracks.each {
            ctx.seqTrackService.decideAndPrepareForAlignment(it)
        }
        //*/
    }
})
null // suppress output spam