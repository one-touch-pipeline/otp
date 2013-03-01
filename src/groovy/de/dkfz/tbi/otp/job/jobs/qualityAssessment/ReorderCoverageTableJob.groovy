package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Project
import org.springframework.beans.factory.annotation.Autowired

class ReorderCoverageTableJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Autowired
    ChromosomeMappingService chromosomeMappingService

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    public void execute() throws Exception {
        long processedBamFileId = Long.parseLong(getProcessParameterValue())
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(processedBamFileId)
        Project project = processedBamFile.alignmentPass.seqTrack.sample.individual.project
        String coverageDataFilePath = processedBamFileQaFileService.coverageDataFilePath(processedBamFile)
        String sortedCoverageDataFilePath = processedBamFileQaFileService.sortedCoverageDataFilePath(processedBamFile)
        Map<String, StringBuilder> chromosome = readAndFilterCoverageData(coverageDataFilePath, project)
        boolean isSortedAndFilteredFileCreated = writeSortedCoverageDataToFile(chromosome, sortedCoverageDataFilePath)
        isSortedAndFilteredFileCreated ? succeed() : fail()
    }

    /**
     * Read and filter the coverage data acording to the project reference genome used
     * It renames the first column(name of the chromosome) and sorting it
     * Only the first column is sorted (The window size is not ordered is taken as it is)
     * Only chromosomes that are mapped at the reference will be considered
     * @param coverageDataFilePath
     * @param project
     * @return Returns a map with coverage data for each chromosome
     */
    private Map readAndFilterCoverageData(String coverageDataFilePath, Project project) {
        String referenceGenomeMapping = processingOptionService.findOption("chromosomeMapping", null, project)
        if (!referenceGenomeMapping) {
            throw new ProcessingException("Undefined reference genome mapping for project ${project.name}")
        }
        Map referenceGenomeMap = chromosomeMappingService."$referenceGenomeMapping"
        File coverageRawDataFile = new File(coverageDataFilePath)
        Map<Integer, StringBuilder> chromosome = [:]
        Integer newChromosomeKey
        coverageRawDataFile.eachLine { String line ->
            List data = line.split("\t", 2)
            newChromosomeKey = referenceGenomeMap[data[0]]
            if (newChromosomeKey) {
                if (!chromosome[newChromosomeKey]) {
                    chromosome[newChromosomeKey] = new StringBuilder()
                }
                chromosome[newChromosomeKey] << "${newChromosomeKey}\t${data[1]}\n"
            }
        }
        return chromosome
    }

    /**
     * Writes the coverage data in a sorted way to a file
     * @param chromosome Mapping of each chromosome to its coverage data
     * @param sortedCoverageDataFilePath Path to where the sorted coverage data will be written
     * @return Returns true if the sorted file was created
     */
    private boolean writeSortedCoverageDataToFile(Map chromosome, String sortedCoverageDataFilePath) {
        List<Integer> sortedChromosomes = chromosome.keySet().sort()
        File sortedCoverageDataFile = new File(sortedCoverageDataFilePath)
        if (sortedCoverageDataFile.exists()) {
                sortedCoverageDataFile.delete()
        }
        sortedCoverageDataFile.createNewFile()
        sortedChromosomes.each { Integer ChromosomeKey ->
            String stats = chromosome.get(ChromosomeKey).toString()
            log.debug "writing chromosome " + ChromosomeKey
            sortedCoverageDataFile << stats
        }
        return (sortedCoverageDataFile.canRead() && sortedCoverageDataFile.size() != 0)
    }
}
