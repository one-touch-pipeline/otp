package de.dkfz.tbi.otp.ngsdata

/**
 * represents different spellings of the {@ling ExomeEnrichmentKit}
 *
 */
class ExomeEnrichmentKitSynonym {

    /**
     * possible spelling of the related {@link ExomeEnrichmentKit}
     */
    String name

    static belongsTo = [
        exomeEnrichmentKit : ExomeEnrichmentKit
    ]

    static constraints = {
        name(unique: true, blank: false)
    }

    static mapping = {
        exomeEnrichmentKit index: "exome_enrichment_kit_synonym_exome_enrichment_kit_idx"
    }
}
