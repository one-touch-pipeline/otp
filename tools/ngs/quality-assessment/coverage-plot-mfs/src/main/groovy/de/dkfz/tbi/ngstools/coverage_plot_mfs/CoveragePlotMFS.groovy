package de.dkfz.tbi.ngstools.coverage_plot_mfs

import groovy.json.JsonSlurper
import javax.validation.*

/**
 * Entry point for the stand alone application for mapping, filtering and sorting coverage files.
 * It reads a coverage file and a JSON file containing mapping, filtering and sorting information,
 * do mapping, filtering and sorting and write the result to a new file.
 *
 */
class CoveragePlotMFS {

    /**
     * constant for the count of expected parameters
     */
    private final static int PARAMETER_COUNT = FileParameters.FIELDS.size()

    /**
     * Info about the parameters inclusive the order.
     * The order and count needs to match the one in {@link FileParameters#FIELDS}.
     */
    private final static String PARAMS_INFO = """
json file: containing mapping, filtering and sorting information
genome coverage file: path to the coverage file, which should be mapped, filtered and sorted
generated genome coverage file: output file for the generated file
override output: if true, the existing output files will be removed
        if false and there are exiting output files, the processing will
        be stopped
"""

    /**
     * error message to use, if the wrong count of parameters are given.
     */
    private final static String PARAM_NUM_ERROR = """\
Number of parameters less then expected: must be ${PARAMETER_COUNT}.
The following parameters are accepted in the following order:
${PARAMS_INFO}
"""

    /**
     * Error message, if the validation fails
     */
    private final static String PARAMS_VALIDATION_FAILED = """
Validation of the input parameters has failed:
"""

    /**
     * Error message, if the processing fails
     */
    private final static String PROCESSING_FAILED = """
Processing has failed:
"""

    /**
     * Error message, if the writing to file fails
     */
    private final static String OUTPUT_FAILED = """
Writing the output files has failed:
"""

    /**
     * Reference to the {@link FileParameters} of the call
     */
    private final FileParameters fileParameters = new FileParameters()

    /**
     * The entry point of this apllication. It handle the complete processing using the different helpers.
     *
     * @param args the parameters of the apllication call
     */
    public static void main(String[] args) {
        CoveragePlotMFS main = new CoveragePlotMFS()
        try {
            main.parseParameters(args)
            main.validateInput()
            main.manageOutputFiles()
            main.runProcessing()
        } catch (Exception e) {
            System.err.println(e.getMessage())
            e.printStackTrace(System.err)
            System.exit(1)
        }
    }

    /**
     * helper to parse parameters to the {@link FileParameters} object using {@link ParameterUtils}
     *
     * @param args the parameters to pass to {@link ParameterUtils}
     */
    private void parseParameters(String[] args) {
        if (args.length < PARAMETER_COUNT) {
            throw new RuntimeException(PARAM_NUM_ERROR)
        }
        ParameterUtils.INSTANCE.parse(fileParameters, fileParameters.FIELDS, args)
    }

    /**
     * helper to validate the input parameters
     */
    private void validateInput() {
        try {
            ParameterUtils.INSTANCE.validate(fileParameters)
            fileParameters.validate()
        } catch (ValidationException e) {
            e.printStackTrace()
            throw new RuntimeException("${PARAMS_VALIDATION_FAILED} ${e.getMessage()}")
        }
    }

    /**
     * helper to manage the output parameters
     */
    private void manageOutputFiles() {
        ParameterUtils.INSTANCE.manageOutputFile(fileParameters.generatedCoverageDataFile, fileParameters.overrideOutput)
    }

    /**
     * do the work of the application.
     */
    private void runProcessing() {
        Map data = readJsonFile()

        //reference the different parts of the map
        Map<String, String> chromosomeIdentifierMap = data["chromosomeIdentifierMap"]
        List<String> filterChromosomes = data["filterChromosomes"]
        List<String> sortedChromosomeIdentifiers = data["sortedChromosomeIdentifiers"]

        //do the work
        println "start loading"
        Map<String, List<String>> coverageData = load()
        println "start mapping identifiers"
        Map<String, List<String>> mappedCoverageData = map(coverageData, chromosomeIdentifierMap)
        println "start filtering"
        Map<String, List<String>> filteredCoverageData = filter(mappedCoverageData, filterChromosomes)
        println "start sorting"
        Map<String, List<String>> sortedAndFilteredIdentifierCoverageData = sort(filteredCoverageData, sortedChromosomeIdentifiers)
        println "start writing file"
        writeCsvFile(sortedAndFilteredIdentifierCoverageData)
        println "finish"
    }

    /**
     * Read JSON string from file and parse it to JSON.
     * The file contains the following informations:
     * <ul>
     * <li> mapping of chromosome names</li>
     * <li> filtering chromosomes</li>
     * <li> sort order of chromosomes</li>
     * </ul>
     *
     * @return the read and parsed JSON file
     */
    private Map readJsonFile() {
        File file = new File(fileParameters.formatingJsonFile)
        String data = file.getText()
        Object json = new JsonSlurper().parseText(data)
        return json
    }

    /**
     * load the coverage in a {@link Map} of {@link String} and {@link List} of {@link String} structure.
     *
     * @return the created map of the loaded file
     */
    private Map<String, List<String>> load() {
        Map<String, List<String>> coverageData = [:]
        File coverageRawDataFile = new File(fileParameters.coverageDataFile)
        coverageRawDataFile.eachLine { String line ->
            List<String> coverageDataPerChromosomePerWindow = line.split("\t", 2)
            String chromosome = coverageDataPerChromosomePerWindow[0]
            String coverage = coverageDataPerChromosomePerWindow[1]
            if (!coverageData[chromosome]) {
                coverageData[chromosome] = []
            }
            coverageData[chromosome] << coverage
        }
        return coverageData
    }

    /**
     * Map the chromosome names of the coverage data structure
     *
     * @param coverageData the coverage data structure with original chromosomes names
     * @param chromosomeIdentifierMap the map containg the chromosome mappings
     * @return the coverage data structure with the mapped chromosome names
     */
    private Map<String, List<String>> map(Map<String, List<String>> coverageData, Map<String, String> chromosomeIdentifierMap) {
        Map<String, List<String>> mappedCoverageData = [:]
        coverageData.each { String chromosome, List<String> coverage ->
            if (chromosomeIdentifierMap.containsKey(chromosome)) {
                String newIdentifier = chromosomeIdentifierMap[chromosome]
                mappedCoverageData[newIdentifier] = coverage
            } else {
                throw new Exception("Chromosome identifier ${chromosome} is not in the mapping yet")
            }
        }
        return mappedCoverageData
    }

    /**
     * Filter out some chromosome names of the coverage data structure
     *
     * @param coverageData the coverage data structure
     * @param filteredChromosomes a map containing the chromosomes, which should be filtered
     * @return a new map only with the chromosome listed in filteredChromosomes
     */
    private Map<String, List<String>> filter(Map<String, List<String>> coverageData, List<String> filteredChromosomes) {
        Map<String, List<String>> filteredCoverageData = coverageData.findAll { Map.Entry<String, List<String>> chromosomeIdentifier ->
            filteredChromosomes.contains(chromosomeIdentifier.key)
        }
        return filteredCoverageData
    }

    /**
     * Sort the chromosome based on the given {@link List} of chromosome names.
     *
     * @param coverageData the coverage data structure
     * @param sortedChromosomeIdentifierList a list with the correct order of the chromosomes
     * @return a sorted map of the chromosomes
     */
    public Map<String, List> sort(Map<String, List> coverageData, List<String> sortedChromosomeIdentifierList) {
        Map sortedCoverageData = [:]
        sortedChromosomeIdentifierList.each { String chromosomeName ->
            if (coverageData[chromosomeName]) {
                sortedCoverageData[chromosomeName] = coverageData[chromosomeName]
            }
        }
        return sortedCoverageData
    }


    /**
     * write the coverage data to the filesystem as tab separated list
     *
     * @param coverageData the coverage data structure to write to file
     */
    private void writeCsvFile(Map<String, List<String>> coverageData) {
        OutputStream outputStream
        try {
            //using an outputstream allow to define also the buffer size
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(fileParameters.generatedCoverageDataFile)), 10000000)
            coverageData.each { String chromosome, List<String> coverage ->
                println "    start writing chromosome: ${chromosome}"
                List<String> coveragePerChromosome = coverage
                coveragePerChromosome.each() { String coveragePerWindow ->
                    outputStream << "${chromosome}\t${coveragePerWindow}\n"
                }
            }
            outputStream.flush()
            outputStream.close()
        } catch (Exception e) {
            println e
            e.printStackTrace()
            if (outputStream) {
                outputStream.close()
            }
            //if exception occur, try to delete already written part of file
            if(!new File(fileParameters.generatedCoverageDataFile).delete()) {
                new File(fileParameters.generatedCoverageDataFile).deleteOnExit()
            }
        }
    }
}
