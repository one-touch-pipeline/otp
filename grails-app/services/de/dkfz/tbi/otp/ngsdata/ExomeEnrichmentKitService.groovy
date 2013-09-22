package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*

/**
 *
 */
class ExomeEnrichmentKitService {

    /**
     * look up the {@link ExomeEnrichmentKit} by its name and by its aliases ({@link ExomeEnrichmentKitIdentifier})
     *
     * @param nameOrAlias the name used for look up
     * @return the found {@link ExomeEnrichmentKit} or <code>null</code>
     */
    public ExomeEnrichmentKit findExomeEnrichmentKitByNameOrAlias(String nameOrAlias) {
        notNull(nameOrAlias, "the input 'nameOrAlias' is null")
        ExomeEnrichmentKit exomeEnrichmentKit = ExomeEnrichmentKit.findByName(nameOrAlias)
        if (!exomeEnrichmentKit) { //not found by name, try aliases
            exomeEnrichmentKit = ExomeEnrichmentKitIdentifier.findByName(nameOrAlias)?.exomeEnrichmentKit
        }
        return exomeEnrichmentKit
    }
}
