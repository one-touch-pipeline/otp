package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack

class RoddySingleLaneQa extends RoddyQualityAssessment {

    SeqTrack seqTrack

    static belongsTo = [
        seqTrack: SeqTrack,
    ]

    static constraints = {
        chromosome(unique: ['qualityAssessmentMergedPass', 'seqTrack'])
        seqTrack(validator: { SeqTrack val, RoddySingleLaneQa obj ->
            obj.roddyBamFile.seqTracks*.id.contains(val.id)
        })
    }
}
