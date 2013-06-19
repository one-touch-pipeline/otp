package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * This service maps the chromosome identifiers of the different reference genomes to OTP identifiers
 *
 */

class ChromosomeIdentifierMappingService {

    final Map<String, String> prefixPerReferenceGenome = ["hg19_1_24" : "chr", "thousandGenomes" : ""]
    /**
     * numericChromosomes includes a list of all numeric OTP chromosome identifiers
     * nonNumericChromosomes includes a list of all non-numeric OTP chromosome identifiers
     */
    final List<String> numericChromosomes = Chromosomes.numericValues()
    final List<String> nonNumericChromosomes = Chromosomes.characterValues()
    ReferenceGenomeFromProjectSeqTypeService referenceGenomeFromProjectSeqTypeService

    public Map<String, String> mappingAll(Project project, SeqType seqType) {
        ReferenceGenome referenceGenome = referenceGenomeFromProjectSeqTypeService.getReferenceGenome(project, seqType)
        return mappingAll(referenceGenome)
    }

    public Map<String, String> mappingAll(ReferenceGenome referenceGenome) {
        Map<String, String> mappedIdentifier = [ : ]
        String referenceGenomeName = referenceGenome.name
        if (prefixPerReferenceGenome[referenceGenome.name]) {
            final List<String> chromosomes = [
                numericChromosomes,
                nonNumericChromosomes
            ].flatten()
            String referenceGenomePrefix = prefixPerReferenceGenome.get(referenceGenomeName)
            chromosomes.each() { chromosome ->
                mappedIdentifier.put(referenceGenomePrefix + chromosome, chromosome)
            }
        } else {
            throw new Exception("Reference Genome ${referenceGenome} is not known yet. Add it in class ChromosomeIdentifierMappingService")
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
