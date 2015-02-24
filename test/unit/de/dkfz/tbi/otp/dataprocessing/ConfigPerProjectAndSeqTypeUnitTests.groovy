package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import grails.test.mixin.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.TestCase.createEmptyTestDirectory

@TestFor(ConfigPerProjectAndSeqType)
@Mock([Project, SeqType])
class ConfigPerProjectAndSeqTypeUnitTests {

    String configuration = "configuration"
    String filePath = "/tmp/otp/otp-test/tempConfigFile.txt"
    ConfigPerProjectAndSeqType validConfigPerProjectAndSeqType

    @Before
    void setUp() {
        validConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
            project: TestData.createProject(),
            seqType: new SeqType(),
            configuration: configuration,
            externalScriptVersion: "v1"
            )
        validConfigPerProjectAndSeqType.save()
    }

    @After
    void tearDown() {
        validConfigPerProjectAndSeqType.delete()
    }

    void testSaveWithoutProject() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                seqType: new SeqType(),
                configuration: configuration,
                externalScriptVersion: "v1",
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.project = TestData.createProject()
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithoutSeqType() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: TestData.createProject(),
                configuration: configuration,
                externalScriptVersion: "v1",
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.seqType = new SeqType()
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithoutConfig() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: TestData.createProject(),
                seqType: new SeqType(),
                externalScriptVersion: "v1",
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.configuration = configuration
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithEmptyConfig() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: TestData.createProject(),
                seqType: new SeqType(),
                configuration: "",
                externalScriptVersion: "v1",
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.configuration = configuration
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithObsoleteDate() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: TestData.createProject(),
                seqType: new SeqType(),
                configuration: configuration,
                obsoleteDate: new Date(),
                externalScriptVersion: "v1",
                )
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSave_noScriptVersion_shouldNotValidate() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: TestData.createProject(),
                seqType: new SeqType(),
                configuration: configuration,
        )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.externalScriptVersion = "v1"
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSave_emptyScriptVersion_shouldNotValidate() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: TestData.createProject(),
                seqType: new SeqType(),
                configuration: configuration,
                externalScriptVersion: ""
        )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.externalScriptVersion = "v1"
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithReferenceToPreviousConfigWithoutObsolete() {
        ConfigPerProjectAndSeqType newConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: TestData.createProject(),
                seqType: new SeqType(),
                configuration: configuration,
                previousConfig: validConfigPerProjectAndSeqType,
                externalScriptVersion: "v1",
                )
        assertFalse(newConfigPerProjectAndSeqType.validate())

        validConfigPerProjectAndSeqType.obsoleteDate = new Date()
        assertTrue(validConfigPerProjectAndSeqType.validate())
        assertTrue(newConfigPerProjectAndSeqType.validate())
    }

    void testWriteToFile_WhenFileDoesNotExistAndOverwriteIsSet_ShouldCreateFile() {
        File configDir = createEmptyTestDirectory()
        File configFile = new File(configDir, 'tempConfigFile.txt')

        try {
            validConfigPerProjectAndSeqType.writeToFile(configFile, true)
            assertEquals(configFile.text, configuration)
        } finally {
            configFile.delete()
            configDir.delete()
        }
    }

    void testWriteToFile_WhenFileDoesExistAndOverwriteIsSet_ShouldCreateFile() {
        File configDir = createEmptyTestDirectory()
        File configFile = new File(configDir, 'tempConfigFile.txt')
        configFile << 'something different'

        try {
            validConfigPerProjectAndSeqType.writeToFile(configFile, true)
            assertEquals(configFile.text, configuration)
        } finally {
            configFile.delete()
            configDir.delete()
        }
    }

    @Test
    void testWriteToFileNoAbsolutePath() {
        shouldFail AssertionError, {
            validConfigPerProjectAndSeqType.writeToFile(new File("tempConfigFile.txt"), false)
        }
    }

    @Test
    void testWriteToFileExistsAlready() {
        File configDir = createEmptyTestDirectory()
        File configFile = new File(configDir, 'tempConfigFile.txt')

        try {
            validConfigPerProjectAndSeqType.writeToFile(configFile, false)
            assertEquals(configFile.text, configuration)
            shouldFail RuntimeException, {
                validConfigPerProjectAndSeqType.writeToFile(configFile, false)
            }
        } finally {
            configFile.delete()
            configDir.delete()
        }
    }
}
