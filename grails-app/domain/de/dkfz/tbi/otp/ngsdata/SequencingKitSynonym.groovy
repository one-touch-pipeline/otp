package de.dkfz.tbi.otp.ngsdata

/**
 * represents different spellings of the {@link SequencingKit}
 *
 */
class SequencingKitSynonym {

    /**
     * possible spelling of the related {@link SequencingKit}
     */
    String name

    static belongsTo = [
        sequencingKit : SequencingKit
    ]

    static constraints = {
        name(unique: true, blank: false)
    }

    static mapping = {
        sequencingKit index: "sequencing_kit_synonym_sequencing_kit_idx"
    }
}
