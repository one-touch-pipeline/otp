package de.dkfz.tbi.otp.dataprocessing

class WriteSortedAndFilteredCoverageDataInFileService {

    /**
     * Method writes the mapped, sorted and filtered coverage data in a file
     * @param filteredCoverageData
     * @param sortedCoverageDataFilePath
     * @return
     */
    private boolean write(Map sortedAndFilteredCoverageData, String sortedAndFilteredCoverageDataFilePath) {
        File sortedCoverageDataFile = new File(sortedAndFilteredCoverageDataFilePath)
        StringBuffer stringBuffer = new StringBuffer()

        for(chromosome in sortedAndFilteredCoverageData) {
            List<String> coverage = chromosome.getValue()
            coverage.each() { String coveragePerWindow ->
                String chromosomeCoverage = chromosome.getKey() + "\t" + coveragePerWindow + "\n"
                log.debug "writing chromosome " + chromosome.getKey()
                stringBuffer.append(chromosomeCoverage)
            }
        }
        String sortedAndFilteredCoverageDataAsString = stringBuffer.toString()
        sortedCoverageDataFile.write(sortedAndFilteredCoverageDataAsString)
        return (sortedCoverageDataFile.canRead() && sortedCoverageDataFile.size() != 0)
    }
}