/**
 * script to trigger alignments for a patient.
 * set the align flag for all runsegments used by seqtracks of the given individuals.
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
            'in'('seqType', SeqType.getAllAlignableSeqTypes())
        }

        //show seqtracks
        seqTracks.each {
            println "${it.id}    ${it.project.name}    ${it.individual.mockPid}     ${it.sampleType.name}    ${it.seqType}    ${it.laneId}    ${it.run.name}    ${it.ilseId}    ${it.libraryPreparationKit}  ${it.seqPlatformGroup}"
        }
        println seqTracks.size()
        println seqTracks*.sample.unique().size()

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
println ''
