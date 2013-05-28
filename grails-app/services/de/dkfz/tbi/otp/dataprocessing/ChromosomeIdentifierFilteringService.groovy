package de.dkfz.tbi.otp.dataprocessing

/**
 * In this service the chromosomes which are not needed can be filtered. For each case a new method should be written, which includes a
 * list with the chromosomes to filter
 * This method calls the general filtering method with the chromosome list as input
 *
 */

class ChromosomeIdentifierFilteringService {

    /**
     * Defines the Chromosomes, which are not needed for the coverage and calls the filtering method
     * @param Map<String, List> sortedIdentifierCoverageData
     *
     */
    public Map<String, List> filteringCoverage(Map<String, List> sortedIdentifierCoverageData) {
        List filterCondition = ["M", "*"]
        return filter(filterCondition, sortedIdentifierCoverageData)
    }

    /**
     * Filters the chromosomes, by the given chromosome identifiers
     *
     * @return
     */
    private Map<String, List> filter(List filterCondition, Map<String, List> sortedIdentifierCoverageData) {
        Map<String, List> filteredIdentifierCoverageData = sortedIdentifierCoverageData.findAll { chromosomeIdentifier ->
            !filterCondition.contains(chromosomeIdentifier.key)
        }
        return filteredIdentifierCoverageData
    }
}
