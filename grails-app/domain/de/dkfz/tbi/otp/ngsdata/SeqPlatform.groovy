package de.dkfz.tbi.otp.ngsdata

class SeqPlatform {

    String name   // eg. solid, illumina
    SeqPlatformModelLabel seqPlatformModelLabel
    SequencingKitLabel sequencingKitLabel

    /**
     * If {@code null}, data from this {@link SeqPlatform} will not be aligned.
     */
    SeqPlatformGroup seqPlatformGroup

    static constraints = {
        name(blank: false, unique: ['seqPlatformModelLabel','sequencingKitLabel'])
        seqPlatformModelLabel(nullable: true)
        sequencingKitLabel(nullable: true)
        seqPlatformGroup(nullable: true)
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
