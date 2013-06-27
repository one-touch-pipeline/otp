package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * This service maps the chromosome identifiers of the different reference genomes to OTP identifiers
 *
 */

class ChromosomeIdentifierMappingService {

    final Map<String, String> prefixPerReferenceGenome = ["hg19_1_24" : "chr", "thousandGenomes" : ""]

    ReferenceGenomeService referenceGenomeService

    public Map<String, String> mappingAll(Project project, SeqType seqType) {
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        return mappingAll(referenceGenome)
    }

    public Map<String, String> mappingAll(ReferenceGenome referenceGenome) {
        Map<String, String> mappedIdentifier = [ : ]
        String referenceGenomeName = referenceGenome.name
        if (prefixPerReferenceGenome[referenceGenomeName]) {
            String referenceGenomePrefix = prefixPerReferenceGenome.get(referenceGenomeName)
            Chromosomes.allLabels().each() { Chromosomes chromosome ->
                mappedIdentifier.put(referenceGenomePrefix + chromosome, chromosome)
            }
        } else {
            throw new Exception("The prefix of the reference Genome ${referenceGenome} is not defined")
        }
        return mappedIdentifier
    }

    public String mappingOne(String referenceGenomeIdentifierForOneChromosome, Project project, SeqType seqType) {
        return mappingAll(project, seqType).get(referenceGenomeIdentifierForOneChromosome)
    }

    public String mappingOne(String referenceGenomeIdentifierForOneChromosome, ReferenceGenome referenceGenome) {
        return mappingAll(referenceGenome).get(referenceGenomeIdentifierForOneChromosome)
    }
}
