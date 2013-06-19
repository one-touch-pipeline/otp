package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import org.springframework.beans.factory.annotation.Autowired

/**
 * In this service the chromosome identifiers of the reference genome (in the coverage data file) are mapped to defined chromosome identifiers. Afterwards the reference genome
 * identifiers are replaced by the new identifiers. These identifiers are sorted and filtered. Finally the coverage data with the new identifiers is
 * written in a new file
 */
class ChromosomeIdentifierProcessingService {

    ProcessedBamFileQaFileService processedBamFileQaFileService
    ProcessingOptionService processingOptionService
    ChromosomeIdentifierMappingService chromosomeIdentifierMappingService
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService
    ChromosomeIdentifierFilteringService chromosomeIdentifierFilteringService

    /**
     * @param processedBamFile
     * @return if the mapping of the reference genome chromosome identifiers to our chromosome identifiers was successfull and when the
     * sorted and filtered chromosome identifiers were written in a file
     */
    public boolean execute(ProcessedBamFile processedBamFile) {
        Project project = processedBamFile.alignmentPass.seqTrack.sample.individual.project
        SeqType seqType = processedBamFile.alignmentPass.seqTrack.seqType
        Map<String, String> chromosomeIdentifierMap = chromosomeIdentifierMappingService.mappingAll(project, seqType)
        String coverageDataFilePath = processedBamFileQaFileService.coverageDataFilePath(processedBamFile)
        Map<String, List<String>> changedIdentifierCoverageData = change(coverageDataFilePath, chromosomeIdentifierMap)
        Map<String, List<String>> filteredCoverageData = chromosomeIdentifierFilteringService.filteringCoverage(changedIdentifierCoverageData)
        Map<String, List<String>> sortedAndFilteredIdentifierCoverageData = chromosomeIdentifierSortingService.sort(filteredCoverageData)
        String sortedAndFilteredCoverageDataFilePath = processedBamFileQaFileService.sortedCoverageDataFilePath(processedBamFile)
        return write(sortedAndFilteredIdentifierCoverageData, sortedAndFilteredCoverageDataFilePath)
    }

    private Map<String, List<String>> change(String coverageDataFilePath, Map<String, String> chromosomeIdentifierMap) {
        Map<String, List<String>> changedIdentifierCoverageData = [:]
        File coverageRawDataFile = new File(coverageDataFilePath)
        coverageRawDataFile.eachLine { String line ->
            List<String> coverageDataPerChromosomePerWindow = line.split("\t", 2)
            if (chromosomeIdentifierMap.containsKey(coverageDataPerChromosomePerWindow[0])) {
                String newIdentifier = chromosomeIdentifierMap[coverageDataPerChromosomePerWindow[0]]
                if (changedIdentifierCoverageData.containsKey(newIdentifier)) {
                    changedIdentifierCoverageData[newIdentifier].add(coverageDataPerChromosomePerWindow[1])
                } else {
                    List<String> coverageDataPerChromosome = []
                    coverageDataPerChromosome.add(coverageDataPerChromosomePerWindow[1])
                    changedIdentifierCoverageData[newIdentifier] = coverageDataPerChromosome
                }
            } else {
                throw new Exception("Chromosome identifier ${coverageDataPerChromosomePerWindow[0]} is not in the mapping yet")
            }
        }
        return changedIdentifierCoverageData
    }

    private boolean write(Map<String, List<String>> sortedAndFilteredCoverageData, String sortedAndFilteredCoverageDataFilePath) {
        File sortedCoverageDataFile = new File(sortedAndFilteredCoverageDataFilePath)
        StringBuffer stringBuffer = new StringBuffer()

        sortedAndFilteredCoverageData.each { chromosome, coverage ->
            List<String> coveragePerChromosome = coverage
            coveragePerChromosome.each() { String coveragePerWindow ->
                String chromosomeCoverage = chromosome + "\t" + coveragePerWindow + "\n"
                stringBuffer.append(chromosomeCoverage)
            }
        }
        String sortedAndFilteredCoverageDataAsString = stringBuffer.toString()
        sortedCoverageDataFile.write(sortedAndFilteredCoverageDataAsString)
        return (sortedCoverageDataFile.canRead() && sortedCoverageDataFile.size() != 0)
    }
}
