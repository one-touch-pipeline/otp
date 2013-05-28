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
    ChromosomeIdentifierReplacingService chromosomeIdentifierReplacingService
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService
    ChromosomeIdentifierFilteringService chromosomeIdentifierFilteringService
    WriteSortedAndFilteredCoverageDataInFileService writeSortedAndFilteredCoverageDataInFileService

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
        Map<String, List> changedIdentifierCoverageData = chromosomeIdentifierReplacingService.change(coverageDataFilePath, chromosomeIdentifierMap)
        Map<String, List> filteredCoverageData = chromosomeIdentifierFilteringService.filteringCoverage(changedIdentifierCoverageData)
        Map<String, List> sortedAndFilteredIdentifierCoverageData = chromosomeIdentifierSortingService.sort(filteredCoverageData)
        String sortedAndFilteredCoverageDataFilePath = processedBamFileQaFileService.sortedCoverageDataFilePath(processedBamFile)
        return writeSortedAndFilteredCoverageDataInFileService.write(sortedAndFilteredIdentifierCoverageData, sortedAndFilteredCoverageDataFilePath)
    }
}
