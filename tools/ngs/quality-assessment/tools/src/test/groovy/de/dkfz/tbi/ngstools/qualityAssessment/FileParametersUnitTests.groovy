package de.dkfz.tbi.ngstools.qualityAssessment

import java.io.File;

import javax.validation.*

import org.junit.*

class FileParametersUnitTests {

    FileParameters fileParameters
    File folder
    File fileInput
    File fileInput2
    File fileInputEmpty
    File fileOutput
    File fileOutputNotReadable

    @Before
    void setUp() throws Exception {
        fileParameters = new FileParameters()
        fileParameters.inputMode = Mode.WGS
        fileParameters.pathQaResulsFile = "test"
        fileParameters.pathCoverateResultsFile = "test"
        fileParameters.pathInsertSizeHistogramFile = "test"
        fileParameters.overrideOutput = true

        folder = new File("/tmp/ParameterUtilsTest/")
        if(folder.exists() == false) {
            folder.mkdir()
        }
        fileInputEmpty = new File(folder.path + "/emptyFile.txt")
        fileInputEmpty << ""
        fileInput = new File(folder.path + "/inputFile1.txt")
        fileInput << "hello world"
        fileInput2 = new File(folder.path + "/inputFile2.txt")
        fileInput2 << "hello world"
        fileOutput = new File(folder.path + "/outFile.txt")
        fileOutput << ""
        fileOutputNotReadable = new File(folder.path + "/notReadableOutFile.jpg")
        fileOutputNotReadable << ""

        fileParameters.pathBamFile = fileInput.path
        fileParameters.pathBamIndexFile = fileInput.path
    }

    @After
    void tearDown() throws Exception {
        fileParameters = null
        folder.deleteDir()
    }

    // fileParameters.bedFilePath tests

    // the test does not fail because the utils are not used
    // for validation of this field
    @Test
    void testBedFilePathValidatedLocally() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = ''
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test
    void testBedFilePathCorrectWgs() {
        fileParameters.validateCaseSpecificInput()
    }

    @Test
    void testBedFilePathCorrectWgsIgnoreExonInput() {
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test
    void testBedFilePathCorrectExon() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testBedFilePathIsNull() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = null
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testBedFilePathIsEmpty() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = ''
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testBedFileNotCorrectPath() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = '/not/existing/path/bedFile.txt'
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testBedFileCanNotRead() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileInput.setReadable(false)
        fileParameters.refGenMetaInfoFilePath = fileInput2.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testBedFileSizeZero() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInputEmpty.path
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    // fileParameters: refGenMetaInfoFilePath tests

    @Test
    void testRefGenMetaInfoFilePathCorrectWgs() {
        fileParameters.validateCaseSpecificInput()
    }

    // the test does not fail because the utils are not used
    // for validation of this field
    @Test
    void testRefGenMetaInfoFilePathValidatedLocally() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = ''
        ParameterUtils.INSTANCE.validate(fileParameters)
    }

    @Test
    void testRefGenMetaInfoFilePathCorrectWgsIgnoreExonInput() {
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test
    void testRefGenMetaInfoFilePathCorrectExon() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = fileInput.path
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testRefGenMetaInfoFilePathIsNull() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = null
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testRefGenMetaInfoFilePathIsEmpty() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = ''
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testRefGenMetaInfoFilePathNotCorrectPath() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = '/not/existing/path/bedFile.txt'
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testRefGenMetaInfoFilePathCanNotRead() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = fileInput2.path
        fileInput2.setReadable(false)
        fileParameters.validateCaseSpecificInput()
    }

    @Test(expected = ValidationException.class)
    void testRefGenMetaInfoFilePathSizeZero() {
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = fileInput.path
        fileParameters.refGenMetaInfoFilePath = fileInputEmpty.path
        fileParameters.validateCaseSpecificInput()
    }

    /*
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
    void testValidateOutputDirecoryPathIsNull() {
        String outputFilePath = null
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = NullPointerException.class)
    void testValidateOutputDirecoryPathIsEmpty() {
        String outputFilePath = ""
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    void testValidateOutputDirecoryFileExistsAndIsNoFile() {
        String outputFilePath = fileOutputNotReadable.getParentFile().path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    void testValidateOutputDirecoryNotReadable() {
        fileOutputNotReadable.getParentFile().setReadable(false)
        fileOutputNotReadable.getParentFile().setWritable(true)
        String outputFilePath = fileOutputNotReadable.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    void testValidateOutputDirecoryNotWriteable() {
        fileOutputNotReadable.getParentFile().setReadable(true)
        fileOutputNotReadable.getParentFile().setWritable(false)
        String outputFilePath = fileOutputNotReadable.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test(expected = ValidationException.class)
    void testValidateOutputDirecoryPathNotExists(){
        File tmpFile = new File("PathNotExists/FileNotExists.txt")
        String outputFilePath = tmpFile.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }

    @Test
    void testValidateOutputDirecoryAllCorrect() {
        fileOutput.getParentFile().setReadable(true)
        fileOutput.getParentFile().setWritable(true)
        String outputFilePath = fileOutput.path
        fileParameters.validateOutputDirecory(outputFilePath)
    }
}
