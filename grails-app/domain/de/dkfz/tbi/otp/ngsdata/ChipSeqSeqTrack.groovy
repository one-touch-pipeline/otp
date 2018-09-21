package de.dkfz.tbi.otp.ngsdata

class ChipSeqSeqTrack extends SeqTrack {

    /**
     * Antibody name or description (too much variability to impose validation rules)
     */
    String antibody

    static constraints = {
        antibody(nullable: true)
    }

    static belongsTo = [
            antibodyTarget: AntibodyTarget,
    ]

    @Override
    String toString() {
        return "${super.toString()} ${antibodyTarget}"
    }

    static mapping = {
        antibodyTarget index: "seq_track_antibody_target_idx"
    }
}
