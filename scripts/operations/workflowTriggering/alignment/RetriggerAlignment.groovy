import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

LogThreadLocal.withThreadLog(System.out, { SeqTrack.withTransaction {

    Collection<SeqTrack> seqTracks = []

    seqTracks.addAll(SeqTrack.withCriteria {
        'in'('id', [
                // SeqTrack IDs
                -1,
        ] as long[])
    })

    seqTracks.addAll(AlignmentPass.withCriteria {
        'in'('id', [
                // AlignmentPass IDs
                -1,
        ] as long[])
    }*.seqTrack)

    seqTracks.addAll(ProcessedBamFile.withCriteria {
        'in'('id', [
                // ProcessedBamFile IDs
                -1,
        ] as long[])
    }*.seqTrack)

    seqTracks.each {
        println "${it.id}    ${it.project.name}    ${it.individual.mockPid}     ${it.sampleType.name}    ${it.seqType}    ${it.laneId}    ${it.run.name}"
    }
    println seqTracks.size()

    /*
    seqTracks.each {
        ctx.seqTrackService.decideAndPrepareForAlignment(it, true)
    }
    //*/
}})
println ''
