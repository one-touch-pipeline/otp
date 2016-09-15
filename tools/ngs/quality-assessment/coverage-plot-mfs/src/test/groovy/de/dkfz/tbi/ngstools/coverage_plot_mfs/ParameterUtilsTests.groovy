package de.dkfz.tbi.ngstools.coverage_plot_mfs

import javax.validation.*
import org.junit.*

//many test are extracted from ParameterUtilsTests and QualityAssessmentStatisticsTest
//from the qa jar
class ParameterUtilsTests {

    /**
     * objects which are used in the class ParameterUtils are either from the type Parameter or FileParameter
     * properties are the properties of Parameter/FileParameter
     */
    FileParameters fileParameters

    File folder
    File fileInputEmpty
    File fileInput
    File fileOutput
    File fileOutputNotReadable
    File fileOutputOverride

    @Before
    public void setUp() throws Exception {
        fileParameters = new FileParameters()
        fileParameters.formatingJsonFile = "json.json"
        fileParameters.coverageDataFile = "coverage.txt"
        fileParameters.generatedCoverageDataFile = "generatedCoverage.txt"
        fileParameters.overrideOutput = true

        folder = new File("/tmp/otp/parameterUtilsTest/")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        folder.setWritable(true)

        fileInputEmpty = new File(folder, "emptyInputFile.txt")
        fileInputEmpty.createNewFile()
        fileInput = new File(folder, "testInputFile.txt")
        fileInput << "hello world"
        fileOutput = new File(folder, "testOutputFile.txt")
        fileOutput.createNewFile()
        fileOutputNotReadable = new File(folder.path, "notReadableTestOutputFile.jpg")
        fileOutputNotReadable.createNewFile()
        fileOutputOverride = new File(folder, "overrideTest.txt")
        fileOutputOverride.createNewFile()

        folder.deleteOnExit()
        fileInputEmpty.deleteOnExit()
        fileInput.deleteOnExit()
        fileOutput.deleteOnExit()
        fileOutputNotReadable.deleteOnExit()
        fileOutputOverride.deleteOnExit()
    }

    @After
    public void tearDown() throws Exception {
        fileParameters = null
        fileInputEmpty.delete()
        fileInput.delete()
        fileOutput.delete()
        fileOutputNotReadable.delete()
        fileOutputOverride.delete()
        folder.deleteDir()
    }

    /**
     * tests the method parseToBoolean with different possible input types, like
     * wrong spelled property, string-value, integer-value, boolean-value
     */
    @Test(expected = MissingPropertyException.class)
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
     * tests the method parseToLong with different possible input types, like
     * wrong spelled property, string-value, boolean-value, integer-value
     */
    @Test(expected = MissingPropertyException.class)
    public void testParseToLongWrongProperty() {
        ParameterUtils.INSTANCE.parseToLong(fileParameters, "referenceGenomeId_unknown", "123")
    }

    @Test(expected = ValidationException.class)
    public void testParseToLongOfString() {
        ParameterUtils.INSTANCE.parseToLong(fileParameters, "referenceGenomeId", "test")
    }

    @Test(expected = ValidationException.class)
    public void testParseToLongOfBoolean() {
        ParameterUtils.INSTANCE.parseToLong(fileParameters, "referenceGenomeId", "true")
    }

    /**
     * tests the method parseToString with different possible input types, like
     * wrong spelled property, string-value
     * in this case the boolean and the integer-value were not tested since the input is always given as a string and therefore,
     * it will be used as a string in this method
     */
    @Test(expected = MissingPropertyException.class)
    public void testParseToStringWrongProperty() {
        ParameterUtils.INSTANCE.parseToString(fileParameters, "unknown", "unknown")
    }

    @Test
    public void testParseToStringAllCorrect() {
        ParameterUtils.INSTANCE.parseToString(fileParameters, "coverageDataFile", "file.txt")
    }

    /**
     * tests the method parse with different possible input types, like
     * string-value, integer-value, boolean-value, wrong spelled property, empty parameter, null
     */
    @Test
    public void testParseString() {
        ParameterUtils.INSTANCE.parse(fileParameters, "coverageDataFile", "file.txt")
    }

    @Test
    public void testParseBoolean() {
        ParameterUtils.INSTANCE.parse(fileParameters, "overrideOutput", "true")
    }

    @Test(expected = MissingPropertyException.class)
    public void testParseWrongParameter() {
        ParameterUtils.INSTANCE.parse(fileParameters, "unknown", "test")
    }

    @Test(expected = MissingPropertyException.class)
    public void testParseEmptyParameter() {
        ParameterUtils.INSTANCE.parse(fileParameters, "", "file.txt")
    }

    @Test(expected = MissingPropertyException.class)
    public void testParseParameterEqualsNull() {
        fileParameters = null
        ParameterUtils.INSTANCE.parse(fileParameters, "coverageDataFile", "file.txt")
    }

    /**
     * tests the method validate with different possible input types, like
     * Parameters is null, FileParameters is null
     * after these two cases the different properties of FileParameters are tested
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateNullFileParameter() {
        fileParameters = null
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    /**
     * validate properties of FileParameters
     */
    @Test
    public void testAllParametersCorrect() {
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testFormatingJsonFileIsNull() {
        fileParameters.formatingJsonFile = null
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testFormatingJsonFileIsEmpty() {
        fileParameters.formatingJsonFile = ""
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testCoverageDataFileIsNull() {
        fileParameters.coverageDataFile = null
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testCoverageDataFileIsEmpty() {
        fileParameters.coverageDataFile = ""
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test(expected = ValidationException.class)
    public void testOverrideOutputIsNull() {
        fileParameters.overrideOutput = null
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    /**
     * Validation of the input files, with different possibilities:
     * - empty InputFilePath
     * - file can not be read
     * - file is empty
     * - input file is correct
     */

    @Test
    public void testValidateInputFileCorrect() {
        ParameterUtils.INSTANCE.validateInputFile(fileInput.path)
    }

    @Test(expected = NullPointerException.class)
    public void testValidateInputFilePathIsNull() {
        ParameterUtils.INSTANCE.validateInputFile(null)
    }

    @Test(expected = ValidationException.class)
    public void testValidateInputFilePathIsEmpty() {
        ParameterUtils.INSTANCE.validateInputFile("")
    }

    @Test(expected = ValidationException.class)
    public void testValidateInputFileCanNotBeRead() {
        fileInput.setReadable(false)
        ParameterUtils.INSTANCE.validateInputFile(fileInput.path)
    }

    @Test(expected = ValidationException.class)
    public void testValidateInputFileIsEmpty() {
        ParameterUtils.INSTANCE.validateInputFile(fileInputEmpty.path)
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

    @Test
    public void testValidateOutputDirecoryAllCorrect() {
        fileOutput.getParentFile().setReadable(true)
        fileOutput.getParentFile().setWritable(true)
        String outputFilePath = fileOutput.path
        ParameterUtils.INSTANCE.validateOutputDirectory(outputFilePath)
    }

    @Test(expected = NullPointerException.class)
    public void testValidateOutputDirecoryPathIsNull() {
        ParameterUtils.INSTANCE.validateOutputDirectory(null)
    }

    @Test(expected = NullPointerException.class)
    public void testValidateOutputDirecoryPathIsEmpty() {
        ParameterUtils.INSTANCE.validateOutputDirectory("")
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryFileExistsAndIsNoFile() {
        String outputFilePath = fileOutputNotReadable.getParentFile().path
        ParameterUtils.INSTANCE.validateOutputDirectory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryNotReadable() {
        fileOutputNotReadable.getParentFile().setReadable(false)
        fileOutputNotReadable.getParentFile().setWritable(true)
        String outputFilePath = fileOutputNotReadable.path
        ParameterUtils.INSTANCE.validateOutputDirectory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryNotWriteable() {
        fileOutputNotReadable.getParentFile().setReadable(true)
        fileOutputNotReadable.getParentFile().setWritable(false)
        String outputFilePath = fileOutputNotReadable.path
        ParameterUtils.INSTANCE.validateOutputDirectory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    public void testValidateOutputDirecoryPathNotExists() {
        File tmpFile = new File("PathNotExists/FileNotExists.txt")
        String outputFilePath = tmpFile.path
        ParameterUtils.INSTANCE.validateOutputDirectory(outputFilePath)
    }

    /**
     * overrideOutput = false
     */
    @Test(expected = RuntimeException.class)
    public void manageOutputFileNoOverride() {
        ParameterUtils.INSTANCE.manageOutputFile(fileOutputOverride.path, false)
    }

    /**
     * overrideOutput = false
     * file.exists = false
     */
    @Test
    public void manageOutputFileNoOverrideFileDoesNotExist() {
        ParameterUtils.INSTANCE.manageOutputFile(fileOutputOverride.path + "_DoesNotExist", false)
    }

    /**
     * overrideOutput = true
     * can not be deleted
     */
    @Test(expected = RuntimeException.class)
    public void manageOutputFileOverrideNoDeletable() {
        fileOutputOverride.setWritable(false)
        fileOutputOverride.getParentFile().setWritable(false)
        ParameterUtils.INSTANCE.manageOutputFile(fileOutputOverride.path, true)
    }

    /**
     * overrideOutput = true
     * file.exists = false
     */
    @Test
    public void manageOutputFileOverrideFileDoesNotExist() {
        ParameterUtils.INSTANCE.manageOutputFile(fileOutputOverride.path + "_DoesNotExist", true)
    }

    /**
     * overrideOutput = true
     * can be deleted
     */
    @Test
    public void manageOutputFileOverrideDeletable() {
        ParameterUtils.INSTANCE.manageOutputFile(fileOutputOverride.path, true)
    }
}
