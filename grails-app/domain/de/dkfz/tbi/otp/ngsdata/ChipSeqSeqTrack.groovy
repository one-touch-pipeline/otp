package de.dkfz.tbi.otp.ngsdata

/**
 * This class represents a ChipSeq seq Track
 *
 */
class ChipSeqSeqTrack extends SeqTrack {

    /**
     * Antibody name or description (too much variability to impose validation rules)
     */
    String antibody

    static constraints = {
        antibody(nullable: true)
    }

    static belongsTo = [
        antibodyTarget: AntibodyTarget
    ]

    public String toString() {
        return "${super.toString()} ${antibodyTarget}"
    }
}
