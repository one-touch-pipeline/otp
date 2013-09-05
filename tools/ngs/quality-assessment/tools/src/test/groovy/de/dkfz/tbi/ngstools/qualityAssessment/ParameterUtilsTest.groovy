package de.dkfz.tbi.ngstools.qualityAssessment

import javax.validation.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.FilesCompletenessService

class ParameterUtilsTest {

    /**
     * objects which are used in the class ParameterUtils are either from the type Parameter or FileParameter
     * properties are the properties of Parameter/FileParameter
     */

    Parameters parameter
    FileParameters fileParameters
    File folder
    File fileInputEmpty
    File fileInput
    File fileOutput
    File fileOutputNotReadable

    @Before
    public void setUp() throws Exception {
        parameter = new Parameters()
        fileParameters = new FileParameters()
        parameter.allChromosomeName = "test"
        parameter.minAlignedRecordLength = 90
        parameter.minMeanBaseQuality = 5
        parameter.mappingQuality = 5
        parameter.winSize = 100
        parameter.binSize = 67895
        parameter.coverageMappingQualityThreshold = 48

        fileParameters.pathBamFile = "test"
        fileParameters.pathBamIndexFile = "test"
        fileParameters.pathQaResulsFile = "test"
        fileParameters.pathCoverateResultsFile = "test"
        fileParameters.pathInsertSizeHistogramFile = "test"
        fileParameters.overrideOutput = true

        folder = new File("/tmp/ParameterUtilsTest/")
        if(folder.exists() == false) {
            folder.mkdir()
        }
        fileInputEmpty = new File(folder.path + "/emptyInputFile.txt")
        fileInputEmpty << ""
        fileInput = new File(folder.path + "/testInputFile.txt")
        fileInput << "hello world"
        fileOutput = new File(folder.path + "/testOutputFile.txt")
        fileOutput << ""
        fileOutputNotReadable = new File(folder.path + "/NotReadableOutputFile.jpg")
        fileOutputNotReadable << ""

        folder.deleteOnExit()
        fileInputEmpty.deleteOnExit()
        fileInput.deleteOnExit()
        fileOutput.deleteOnExit()
        fileOutputNotReadable.deleteOnExit()
    }

    @After
    public void tearDown() throws Exception {
        parameter = null
        fileParameters = null
        folder.deleteDir()
        fileInputEmpty.delete()
        fileInput.delete()
        fileOutput.delete()
        fileOutputNotReadable.delete()
    }

    /**
     * tests the method parseToBoolean with different possible input types, like
     * wrong spelled property, string-value, integer-value, boolean-value
     */
    @Test(expected = ValidationException.class)
    public void testParseToBooleanWrongProperty() {
        ParameterUtils.INSTANCE.parseToBoolean(fileParameters, "overrideOutpu", "true")
    }

    @Test(expected = ValidationException.class)
    public void testParseToBooleanOfString() {
        ParameterUtils.INSTANCE.parseToBoolean(fileParameters, "overrideOutput", "test")
    }

    @Test(expected = ValidationException.class)
    public void testParseToBooleanOfInteger() {
        ParameterUtils.INSTANCE.parseToBoolean(fileParameters, "overrideOutput", "123")
    }

    @Test
    public void testParseToBooleanTrueAllCorrect() {
        ParameterUtils.INSTANCE.parseToBoolean(fileParameters, "overrideOutput", "true")
    }

    @Test
    public void testParseToBooleanFalseAllCorrect() {
        ParameterUtils.INSTANCE.parseToBoolean(fileParameters, "overrideOutput", "false")
    }


    /**
     * tests the method parseToInteger with different possible input types, like
     * wrong spelled property, string-value, boolean-value, integer-value
     */
    @Test(expected = ValidationException.class)
    public void testParseToIntegerWrongProperty() {
        ParameterUtils.INSTANCE.parseToInteger(parameter, "minAlignedRecordLen", "123")
    }

    @Test(expected = ValidationException.class)
    public void testParseToIntegerOfString() {
        ParameterUtils.INSTANCE.parseToInteger(parameter, "minAlignedRecordLength", "test")
    }

    @Test(expected = ValidationException.class)
    public void testParseToIntegerOfBoolean() {
        ParameterUtils.INSTANCE.parseToInteger(parameter, "minAlignedRecordLength", "true")
    }

    @Test
    public void testParseToIntegerAllCorrect() {
        ParameterUtils.INSTANCE.parseToInteger(parameter, "minAlignedRecordLength", "123")
    }


    /**
     * tests the method parseToString with different possible input types, like
     * wrong spelled property, string-value
     * in this case the boolean and the integer-value were not tested since the input is always given as a string and therefore,
     * it will be used as a string in this method
     */
    @Test(expected = MissingPropertyException.class)
    public void testParseToStringWrongProperty() {
        ParameterUtils.INSTANCE.parseToString(parameter, "allChromosomeNa", "Chromosome_X")
    }

    @Test
    public void testParseToStringAllCorrect() {
        ParameterUtils.INSTANCE.parseToString(parameter, "allChromosomeName", "Chromosome_X")
    }


    /**
     * tests the method parse with different possible input types, like
     * string-value, integer-value, boolean-value, wrong spelled property, empty parameter, null
     */
    @Test
    public void testParseString() {
        ParameterUtils.INSTANCE.parse(parameter, "allChromosomeName", "Chromosome_X")
    }

    @Test
    public void testParseInteger() {
        ParameterUtils.INSTANCE.parse(parameter, "minAlignedRecordLength", "123")
    }

    @Test
    public void testParseBoolean() {
        ParameterUtils.INSTANCE.parse(fileParameters, "overrideOutput", "true")
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongParameter() {
        ParameterUtils.INSTANCE.parse(parameter, "allChromosomeNam", "Chromosome_X")
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmptyParameter() {
        ParameterUtils.INSTANCE.parse(parameter, "", "Chromosome_X")
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseParameterEqualsNull() {
        String parameter = null
        ParameterUtils.INSTANCE.parse(parameter, "allChromosomeName", "Chromosome_X")
    }


    /**
     * tests the method validate with different possible input types, like
     * Parameters is null, FileParameters is null
     * after these two cases the different properties of Parameters/FileParameters are tested
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateNullParameter() {
        parameter = null
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateNullFileParameter() {
        fileParameters = null
        ParameterUtils.INSTANCE.validate(fileParameters)
    }


    /**
     * validate properties of Parameters
     */
    /**
     * different values of the property "allChromosomeName" are tested:
     * "test", null, empty, name is too long
     * only this property is tested for null, since it is declared as NotNull
     */
    @Test
    public void testAllChromosomeNameCorrect() {
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testAllChromosomeNameIsNull() {
        parameter.allChromosomeName = null
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testAllChromosomeNameIsEmpty() {
        parameter.allChromosomeName = ""
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testAllChromosomeNameIsToLong() {
        parameter.allChromosomeName = "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest"
        ParameterUtils.INSTANCE.validate(parameter)
    }

    /**
     * different values of the property "minAlignedRecordLength" are tested:
     * 90, too short (-3), too long (9999999) 
     */
    @Test
    public void testMinAlignedRecordLengthCorrect() {
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testMinAlignedRecordLengthToShort() {
        parameter.minAlignedRecordLength = -3
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testMinAlignedRecordLengthToLong() {
        parameter.minAlignedRecordLength = 9999999
        ParameterUtils.INSTANCE.validate(parameter)
    }

    /**
     * different values of the property "minMeanBaseQuality" are tested:
     * 5, too low (-3), too high (9999999)
     */
    @Test
    public void testMinMeanBaseQualityCorrect() {
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testMinMeanBaseQualityToLow() {
        parameter.minMeanBaseQuality = -3
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testMinMeanBaseQualityToHigh() {
        parameter.minMeanBaseQuality = 9999999
        ParameterUtils.INSTANCE.validate(parameter)
    }

    /**
     * different values of the property "mappingQuality" are tested:
     * 5, too low (-3), too high (9999999)
     */
    @Test
    public void testMappingQualityCorrect() {
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testMappingQualityToLow() {
        parameter.mappingQuality = -3
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testMappingQualityToHigh() {
        parameter.mappingQuality = 9999999
        ParameterUtils.INSTANCE.validate(parameter)
    }

    /**
     * different values of the property "winSize" are tested:
     * 90, too short (-3), too long (9999999)
     */
    @Test
    public void testWinSizeCorrect() {
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testWinSizeToShort() {
        parameter.winSize = -3
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testWinSizeToLong() {
        parameter.winSize = 9999999
        ParameterUtils.INSTANCE.validate(parameter)
    }

    /**
     * different values of the property "binSize" are tested:
     * 67895, too short (-3), too long (99999999999)
     */
    @Test
    public void testBinSizeCorrect() {
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testBinSizeToShort() {
        parameter.binSize = -3
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testBinSizeToLong() {
        parameter.binSize = 99999999999
        ParameterUtils.INSTANCE.validate(parameter)
    }

    /**
     * different values of the property "coverageMappingQualityThreshold" are tested:
     * 48, too low (-3), too high (99999999999)
     */
    @Test
    public void testCoverageMappingQualityThresholdCorrect() {
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testCoverageMappingQualityThresholdToLow() {
        parameter.coverageMappingQualityThreshold = -3
        ParameterUtils.INSTANCE.validate(parameter)
    }

    @Test(expected = ValidationException.class)
    public void testCoverageMappingQualityThresholdToHigh() {
        parameter.coverageMappingQualityThreshold = 99999999999
        ParameterUtils.INSTANCE.validate(parameter)
    }


    /**
     * validate properties of FileParameters
     */
    /**
     * different values of the property "pathBamFile" are tested:
     * "test", empty
     */
    @Test
    public void testPathBamFileCorrect() {
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testPathBamFileToShort() {
        fileParameters.pathBamFile = ""
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    /**
     * different values of the property "pathBamIndexFile" are tested:
     * "test", empty
     */
    @Test
    public void testPathBamIndexFileCorrect() {
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testPathBamIndexFileToShort() {
        fileParameters.pathBamIndexFile = ""
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    /**
     * different values of the property "pathQaResulsFile" are tested:
     * "test", empty
     */
    @Test
    public void testPathQaResulsFileCorrect() {
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testPathQaResulsFileToShort() {
        fileParameters.pathQaResulsFile = ""
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    /**
     * different values of the property "pathCoverateResultsFile" are tested:
     * "test", empty
     */
    @Test
    public void testPathCoverateResultsFileCorrect() {
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testPathCoverateResultsFileToShort() {
        fileParameters.pathCoverateResultsFile = ""
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    /**
     * different values of the property "pathInsertSizeHistogramFile" are tested:
     * "test", empty
     */
    @Test
    public void testPathInsertSizeHistogramFileCorrect() {
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testPathInsertSizeHistogramFileToShort() {
        fileParameters.pathInsertSizeHistogramFile = ""
        ParameterUtils.INSTANCE.validate(fileParameters)
    }


    @Test
    public void testOverrideOutputCorrect() {
        ParameterUtils.INSTANCE.validate(fileParameters)
    }


    /**
     * Validation of the input files, with different possibilities:
     * - empty InputFilePath
     * - file can not be read
     * - file is empty
     * - input file is correct
     */
    @Test(expected = ValidationException.class)
    public void testValidateInputFilePathIsEmpty() {
        String inputFilePath = ""
        fileParameters.validateInputFile(inputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateInputFileCantBeRead() {
        fileInputEmpty.setReadable(false)
        String inputFilePath = fileInputEmpty.path
        fileParameters.validateInputFile(inputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateInputFileIsEmpty() {
        String inputFilePath = fileInputEmpty.path
        fileParameters.validateInputFile(inputFilePath)
    }

    @Test
    public void testValidateInputFileCorrect() {
        String inputFilePath = fileInput.path
        fileParameters.validateInputFile(inputFilePath)
    }


    /**
     * Validation of the output files, with different possibilities:
     * - output file path is null
     * - output file path is empty
     * - output file exists, but is no correct file
     * - output file directory is not readable
     * - output file directory is not writeable
     * - output file directory does not exist
     * - output file is correct
     */
    @Test(expected = NullPointerException.class)
    public void testValidateOutputDirecoryPathIsNull() {
        String outputFilePath = null
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = NullPointerException.class)
    public void testValidateOutputDirecoryPathIsEmpty() {
        String outputFilePath = ""
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryFileExistsAndIsNoFile() {
        String outputFilePath = fileOutputNotReadable.getParentFile().path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryNotReadable() {
        fileOutputNotReadable.getParentFile().setReadable(false)
        fileOutputNotReadable.getParentFile().setWritable(true)
        String outputFilePath = fileOutputNotReadable.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryNotWriteable() {
        fileOutputNotReadable.getParentFile().setReadable(true)
        fileOutputNotReadable.getParentFile().setWritable(false)
        String outputFilePath = fileOutputNotReadable.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryPathNotExists(){
        File tmpFile = new File("PathNotExists/FileNotExists.txt")
        String outputFilePath = tmpFile.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test
    public void testValidateOutputDirecoryAllCorrect() {
        fileOutput.getParentFile().setReadable(true)
        fileOutput.getParentFile().setWritable(true)
        String outputFilePath = fileOutput.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }
}
