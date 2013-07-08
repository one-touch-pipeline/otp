package de.dkfz.tbi.otp.dataprocessing

/**
 * In this service the chromosomes which are not needed can be filtered. For each case a new method should be written, which includes a
 * list with the chromosomes to filter
 * This method calls the general filtering method with the chromosome list as input
 *
 */

class ChromosomeIdentifierFilteringService {

    /**
     * returns the chromosome names to filter out for coverage
     */
    public List<String> filteringCoverage() {
        return Chromosomes.asteriskAndMLabels()
    }
}
