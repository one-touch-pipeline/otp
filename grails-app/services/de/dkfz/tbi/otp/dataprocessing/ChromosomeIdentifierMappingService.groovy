package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * This service maps the chromosome identifiers of the different reference genomes to OTP identifiers
 *
 */
class ChromosomeIdentifierMappingService {

    ReferenceGenomeService referenceGenomeService



    Map<String, String> mappingAll(ReferenceGenome referenceGenome) {
        notNull(referenceGenome, "referenceGenome in method mappingAll is null")
        Map<String, String> mappedIdentifier = [:]
        List<ReferenceGenomeEntry> referenceGenomeEntries = ReferenceGenomeEntry.findAllByReferenceGenome(referenceGenome)
        referenceGenomeEntries.each { ReferenceGenomeEntry referenceGenomeEntry ->
            mappedIdentifier.put(referenceGenomeEntry.name, referenceGenomeEntry.alias)
        }
        mappedIdentifier.put("*", "*")
        return mappedIdentifier
    }

    String mappingOne(String referenceGenomeIdentifierForOneChromosome, ReferenceGenome referenceGenome) {
        notNull(referenceGenomeIdentifierForOneChromosome, "identifier ist null")
        notNull(referenceGenome, "referenceGenome in method mappingOne is null")
        return mappingAll(referenceGenome).get(referenceGenomeIdentifierForOneChromosome)
    }
}
