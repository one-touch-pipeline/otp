package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.SeqType

/**
 * This service maps the chromosome identifiers of the different reference genomes to defined identifiers
 *
 */

class ChromosomeIdentifierMappingService {

    final Map<String, String> prefixPerReferenceGenome = ["hg19_1_24" : "chr", "thousandGenomes" : ""]
    final List<String> numericChromosomes = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
         "16", "17", "18", "19", "20", "21", "22"]
    final List<String> nonNumericChromosomes = ["X", "Y", "M", "*"]
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    /**
     * @param project
     * @param seqType
     * @return mapping of the reference genome specific chromosome identifiers to defined chromosome identifiers depending on the
     * project and the sequencing type
     */
    public Map<String, String> mappingAll(Project project, SeqType seqType) {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.withCriteria(uniqueResult: true) {
            eq("project", project)
            eq("seqType", seqType)
        }
        ReferenceGenome referenceGenome = referenceGenomeProjectSeqType.referenceGenome
        return mappingAll(referenceGenome)
    }

    /**
     * @param referenceGenome
     * @return mapping of the reference genome specific chromosome identifiers to defined chromosome identifiers depending on the
     * referenceGenome
     */
    public Map<String, String> mappingAll(ReferenceGenome referenceGenome) {
        Map<String, String> mappedIdentifier = [ : ]
        String referenceGenomeName = referenceGenome.name
        if (prefixPerReferenceGenome[referenceGenome.name] != null) {
            final List<String> chromosomes = [
                numericChromosomes,
                nonNumericChromosomes
            ].flatten()
            String referenceGenomePrefix = prefixPerReferenceGenome.get(referenceGenomeName)
            chromosomes.each() { chromosome ->
                mappedIdentifier.put(referenceGenomePrefix + chromosome, chromosome)
            }
        } else {
            throw new Exception("Reference Genome " + referenceGenome + "is not known yet. Add it in class ChromosomeIdentifierMappingService")
        }
        return mappedIdentifier
    }

    /**
     * @param Identifier
     * @param project
     * @param seqType
     * @return the mapped identifier of one chromosome, which belongs to the input reference genome identifier for this chromosome
     */
    public String mappingOne(String referenceGenomeIdentifierForOneChromosome, Project project, SeqType seqType) {
        return mappingAll(project, seqType).get(referenceGenomeIdentifierForOneChromosome)
    }

    /**
     * @param Identifier
     * @param referenceGenome
     * @return the mapped identifier of one chromosome, which belongs to the input reference genome identifier for this chromosome
     */
    public String mappingOne(String referenceGenomeIdentifierForOneChromosome, ReferenceGenome referenceGenome) {
        return mappingAll(referenceGenome).get(referenceGenomeIdentifierForOneChromosome)
    }
}
