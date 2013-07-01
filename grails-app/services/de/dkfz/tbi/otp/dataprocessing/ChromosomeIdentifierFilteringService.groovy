package de.dkfz.tbi.otp.dataprocessing

/**
 * In this service the chromosomes which are not needed can be filtered. For each case a new method should be written, which includes a
 * list with the chromosomes to filter
 * This method calls the general filtering method with the chromosome list as input
 *
 */

class ChromosomeIdentifierFilteringService {

    public Map<String, List> filteringCoverage(Map<String, List<String>> sortedIdentifierCoverageData) {
        List<String> filterCondition = Chromosomes.asteriskAndMLabels()
        return filter(filterCondition, sortedIdentifierCoverageData)
    }

    private Map<String, List<String>> filter(List<String> filterCondition, Map<String, List<String>> sortedIdentifierCoverageData) {
        Map<String, List<String>> filteredIdentifierCoverageData = sortedIdentifierCoverageData.findAll { chromosomeIdentifier ->
            !filterCondition.contains(chromosomeIdentifier.key)
        }
        return filteredIdentifierCoverageData
    }
}
