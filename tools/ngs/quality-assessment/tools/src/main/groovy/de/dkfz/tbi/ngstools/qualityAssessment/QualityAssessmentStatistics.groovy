package de.dkfz.tbi.ngstools.qualityAssessment

import javax.validation.ValidationException
import net.sf.samtools.SAMRecord


class QualityAssessmentStatistics {

    /**
     * number of parameter to call the qa.jar for whole genome
     * @see #PARAMETER_COUNT_EXOME PARAMETER_COUNT_EXOME
     */
    private final static int PARAMETER_COUNT_WGS = 14
    /**
     * number of parameter to call the qa.jar for exome
     * @see #PARAMETER_COUNT_WGS PARAMETER_COUNT_WGS
     */
    private final static int PARAMETER_COUNT_EXOME = 16

    private final static Parameters parameters = new Parameters()

    private final static FileParameters fileParameters = new FileParameters()

    private final static String PARAMS_INFO = """
Path to the BAM file (input)
Path to the index (bai) file (input)
Path to quality assessment result file (output, json)
Path to genome coverage file (output, for coverage plot)
Path to insert sizes histogram file (output, for insert size plot)
remove_output: if true, the existing output files will be removed
        if false and there are exiting output files, the processing will
        be stopped
Alias for the "ALL" chromosome. It cannot contain empty spaces. (whole genome)
MinAlignedRecordLength - minimun length a read should aligned to the reference (Integer between 0 and 10000. common value: 36).
MinMeanBaseQuality - used to decide if read is mapped with good Quality or low Quality (Integer between 0 and 100. common value: 0).
MappingQuality - used to decide if read is mapped at all and included into mapping calculation (Integer between 0 and 100. common value: 0).
Coverage Mapping Quality Threshold - used to decide if read is added for the coverage plot. (Integer between 0 and 100. common value: 1).
Window Size - Window size for the coverage plot in bp. A small window size require higher memory and processing time. (Integer between 100 and 10000. common value: 1000).
BinSize - Basket size for the insert size histogram (Integer between 1 and 1000000. common value: 10).
bedFilePath - Path to the BED file which only holds the required fields: chrom, chromStart, chromEnd
TestMode - Should the test mode be used. In the test mode the COV parameters of some chromosomes (*, m, chrM) are
        filtered out for counting for the "ALL" chromosome
For Exome:
Path to the BED file (input)
Path to the file containing the names of the reference genome entries
"""

    private final static String PARAM_NUM_ERROR = """\
Number of parameters less then expected: must be ${PARAMETER_COUNT_WGS} for whole genome
or ${PARAMETER_COUNT_EXOME} for exome.
The following parameters are accepted in the following order:
${PARAMS_INFO}
"""

    private final static String PARAMS_VALIDATION_FAILED = """
Validation of the input parameters has failed:
"""

    private final static String OUTPUT_FILE_EXISTS = """
The output file exists and overrideOutput is set to FALSE:
"""

    private final static String CAN_NOT_DELETE_OUTPUT_FILE = """
The old output file can not be deleted:
"""

    private final static String PROCESSING_FAILED = """
Processing has failed:
"""

    private final static String OUTPUT_FAILED = """
Writing the output files has failed:
"""

    /**
     * @param args
     */
    public static void main(String[] args) {
        try{
            run(args)
        }catch(Exception e){
            exitWithError(e.getMessage())
        }
    }

    // this method is used to run integration tests;
    // problem: we do not want to System.exit() tests on exception;
    // better solution would be mocking of the exitWithError() in the tests.
    // QualityAssessmentStatistics.metaClass.'static'.exitWithError = { String message -> }
    // does not override the method.
    // Because of lack of time, this solution has been used.
    private static void run(String[] args) {
        parseParameters(args)
        validateInput()
        manageOutputFiles()
        GenomeStatistic<?> genomeStatistic = runProcessing()
        writeOutput(genomeStatistic)
    }

    private static void parseParameters(String[] args) {
        if ((args.length != PARAMETER_COUNT_WGS) && (args.length != PARAMETER_COUNT_EXOME)) {
            throw new RuntimeException(PARAM_NUM_ERROR)
        }
        fileParameters.inputMode = Mode.WGS
        parameters.inputMode = Mode.WGS
        ParameterUtils.INSTANCE.parse(fileParameters, "pathBamFile", args[0])
        ParameterUtils.INSTANCE.parse(fileParameters, "pathBamIndexFile", args[1])
        ParameterUtils.INSTANCE.parse(fileParameters, "pathQaResulsFile", args[2])
        ParameterUtils.INSTANCE.parse(fileParameters, "pathCoverateResultsFile", args[3])
        ParameterUtils.INSTANCE.parse(fileParameters, "pathInsertSizeHistogramFile", args[4])
        ParameterUtils.INSTANCE.parse(fileParameters, "overrideOutput", args[5])
        ParameterUtils.INSTANCE.parse(parameters, "allChromosomeName", args[6])
        ParameterUtils.INSTANCE.parse(parameters, "minAlignedRecordLength", args[7])
        ParameterUtils.INSTANCE.parse(parameters, "minMeanBaseQuality", args[8])
        ParameterUtils.INSTANCE.parse(parameters, "mappingQuality", args[9])
        ParameterUtils.INSTANCE.parse(parameters, "coverageMappingQualityThreshold", args[10])
        ParameterUtils.INSTANCE.parse(parameters, "winSize", args[11])
        ParameterUtils.INSTANCE.parse(parameters, "binSize", args[12])
        ParameterUtils.INSTANCE.parse(parameters, "testMode", args[13])
        if(args.length == PARAMETER_COUNT_EXOME) {
            fileParameters.inputMode = Mode.EXOME
            parameters.inputMode = Mode.EXOME
            ParameterUtils.INSTANCE.parse(fileParameters, "bedFilePath", args[14])
            ParameterUtils.INSTANCE.parse(fileParameters, "refGenMetaInfoFilePath", args[15])
        }
    }

    private static void validateInput() {
        try {
            ParameterUtils.INSTANCE.validate(parameters)
            ParameterUtils.INSTANCE.validate(fileParameters)
            fileParameters.validateFiles()
        } catch (ValidationException e) {
            e.printStackTrace()
            throw new RuntimeException("${PARAMS_VALIDATION_FAILED} ${e.getMessage()}")
        }
    }

    private static void manageOutputFiles() {
        manageOutputFile(fileParameters.pathQaResulsFile, fileParameters.overrideOutput)
        manageOutputFile(fileParameters.pathCoverateResultsFile, fileParameters.overrideOutput)
        manageOutputFile(fileParameters.pathInsertSizeHistogramFile, fileParameters.overrideOutput)
    }

    private static void manageOutputFile(String path, boolean overrideOutput) {
        File file = new File(path)
        if (!overrideOutput && file.exists()) {
            throw new RuntimeException("${OUTPUT_FILE_EXISTS} ${path}")
        }
        if (overrideOutput && file.exists()) {
            Boolean deleted = file.delete()
            if (!deleted) {
                throw new RuntimeException("${CAN_NOT_DELETE_OUTPUT_FILE} ${path}")
            }
        }
    }

    private static GenomeStatistic<?> runProcessing() {
        BamFileReader<SAMRecord> reader = new SAMBamFileReaderImpl()
        GenomeStatisticFactory<SAMRecord> factory = new SAMGenomeStatisticFactory()
        reader.setParameters(parameters)
        reader.setFileParameters(fileParameters)
        reader.setGenomeStatisticFactory(factory)
        try {
            File bamFile = new File(fileParameters.pathBamFile)
            File indexFile = new File(fileParameters.pathBamIndexFile)
            return reader.read(bamFile, indexFile)
        } catch (Exception e) {
            e.printStackTrace()
            throw new RuntimeException("${PROCESSING_FAILED}: ${e.getMessage()}")
        }
    }

    private static void writeOutput(GenomeStatistic<?> genomeStatistic) {
        CoverageWriter coverageWriter = new CoverageWriter()
        HistogramWriter histogramWriter = new HistogramWriter()
        StatisticWriter statisticWriter = new StatisticWriter()

        coverageWriter.setParameters(parameters)
        histogramWriter.setParameters(parameters)
        statisticWriter.setParameters(parameters)

        try {
            File file = new File(fileParameters.pathCoverateResultsFile)
            coverageWriter.write(file, genomeStatistic)

            file = new File (fileParameters.pathInsertSizeHistogramFile)
            histogramWriter.write(file, genomeStatistic)

            file = new File (fileParameters.pathQaResulsFile)
            statisticWriter.write(file, genomeStatistic)
        } catch (Exception e) {
            e.printStackTrace()
            throw new RuntimeException("${OUTPUT_FAILED}: ${e.getMessage()}")
        }
    }

    private static void exitWithError(String message) {
        System.err.println(message)
        System.exit(1)
    }
}
