package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry

/**
 * In this service the chromosomes which are not needed can be filtered. For each case a new method should be written, which includes a
 * list with the chromosomes to filter
 * This method calls the general filtering method with the chromosome list as input
 *
 */

class ChromosomeIdentifierFilteringService {

    ReferenceGenomeService referenceGenomeService

    /**
     * returns the chromosome names to filter out for coverage
     */
    public List<String> filteringCoverage(ReferenceGenome referenceGenome) {
        notNull(referenceGenome, "the referenceGenome in method filteringCoverage is null")
        List<ReferenceGenomeEntry> referenceGenomeEntries = referenceGenomeService.chromosomesInReferenceGenome(referenceGenome)
        return referenceGenomeEntries*.alias
    }
}
