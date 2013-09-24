package de.dkfz.tbi.otp.ngsdata

class ExomeSeqTrack extends SeqTrack {

    static belongsTo = [
        exomeEnrichmentKit: ExomeEnrichmentKit
    ]

    public String toString() {
        return "${super.toString()} ${exomeEnrichmentKit}"
    }
}
