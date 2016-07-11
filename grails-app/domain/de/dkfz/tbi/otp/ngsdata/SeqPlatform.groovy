package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class SeqPlatform implements Entity {

    String name   // eg. solid, illumina
    SeqPlatformModelLabel seqPlatformModelLabel
    SequencingKitLabel sequencingKitLabel

    /**
     * If {@code null}, data from this {@link SeqPlatform} will not be aligned.
     */
    SeqPlatformGroup seqPlatformGroup

    String identifierInRunName

    static constraints = {
        name(blank: false, unique: ['seqPlatformModelLabel','sequencingKitLabel'])
        seqPlatformModelLabel(nullable: true)
        sequencingKitLabel(nullable: true)
        seqPlatformGroup(nullable: true)
        identifierInRunName(nullable: true, matches: /^[A-Z]{4}$/)
    }

    String toString() {
        return fullName()
    }

    String fullName() {
        return [
            name,
            seqPlatformModelLabel?.name,
            sequencingKitLabel?.name ?: 'unknown kit'
            ].findAll().join(' ')
    }

    static mapping = {
        sequencingKitLabel index: "seq_platform_sequencing_kit_label_idx"
        seqPlatformModelLabel index: "seq_platform_seq_platform_model_label_idx"
    }
}
