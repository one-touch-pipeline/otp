package de.dkfz.tbi.otp.ngsdata

/**
 * represents different spellings of the {@ling ExomeEnrichmentKit}
 *
 */
class ExomeEnrichmentKitIdentifier {

    /**
     * possible spelling of the related {@ling ExomeEnrichmentKit}
     */
    String name

    static belongsTo = [
        exomeEnrichmentKit : ExomeEnrichmentKit
    ]

    static constraints = {
        name(unique: true, blank: false)
    }
}
