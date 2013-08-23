package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * This service maps the chromosome identifiers of the different reference genomes to OTP identifiers
 *
 */
class ChromosomeIdentifierMappingService {

    ReferenceGenomeService referenceGenomeService

    public Map<String, String> mappingAll(Project project, SeqType seqType) {
        notNull(project, "project in method mappingAll is null")
        notNull(seqType, "seqType in method mappingAll is null")
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        return mappingAll(referenceGenome)
    }

    public Map<String, String> mappingAll(ReferenceGenome referenceGenome) {
        notNull(referenceGenome, "referenceGenome in method mappingAll is null")
        Map<String, String> mappedIdentifier = [:]
        List<ReferenceGenomeEntry> referenceGenomeEntries = ReferenceGenomeEntry.findAllByReferenceGenome(referenceGenome)
        referenceGenomeEntries.each { ReferenceGenomeEntry referenceGenomeEntry ->
            mappedIdentifier.put(referenceGenomeEntry.name, referenceGenomeEntry.alias)
        }
        mappedIdentifier.put("*", "*")
        return mappedIdentifier
    }

    public String mappingOne(String referenceGenomeIdentifierForOneChromosome, Project project, SeqType seqType) {
        notNull(project, "project in method mappingOne is null")
        notNull(seqType, "seqType in method mappingOne is null")
        notNull(referenceGenomeIdentifierForOneChromosome, "identifier ist null")
        return mappingAll(project, seqType).get(referenceGenomeIdentifierForOneChromosome)
    }

    public String mappingOne(String referenceGenomeIdentifierForOneChromosome, ReferenceGenome referenceGenome) {
        notNull(referenceGenomeIdentifierForOneChromosome, "identifier ist null")
        notNull(referenceGenome, "referenceGenome in method mappingAll is null")
        return mappingAll(referenceGenome).get(referenceGenomeIdentifierForOneChromosome)
    }
}
