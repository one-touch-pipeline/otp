package de.dkfz.tbi.otp.dataprocessing

class ChromosomeIdentifierReplacingService {

    /**
     * Method, which reads the file with the coverage data and replaces the reference genome specific chromosome identifiers with the
     * defined identifier
     * @param coverageDataFilePath
     * @param chromosomeIdentifierMap
     * @return map with the content of the coverageDataFile but the reference genome specific identifiers
     *  are replaced by defined identifiers
     */
    public Map<String, List> change(String coverageDataFilePath, Map chromosomeIdentifierMap) {
        Map<String, List> changedIdentifierCoverageData = [:]
        File coverageRawDataFile = new File(coverageDataFilePath)
        coverageRawDataFile.eachLine { String line ->
            List coverageDataPerChromosomePerWindow = line.split("\t", 2)
            String newIdentifier = chromosomeIdentifierMap[coverageDataPerChromosomePerWindow[0]]
            List<String> coverageDataPerChromosome = []
            if (! (changedIdentifierCoverageData[newIdentifier] == null)) {
                coverageDataPerChromosome = changedIdentifierCoverageData[newIdentifier]
            }
            coverageDataPerChromosome.add(coverageDataPerChromosomePerWindow[1])
            changedIdentifierCoverageData[newIdentifier] = coverageDataPerChromosome
        }
        return changedIdentifierCoverageData
    }
}
