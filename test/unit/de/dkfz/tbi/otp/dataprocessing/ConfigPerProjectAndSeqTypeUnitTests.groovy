package de.dkfz.tbi.otp.dataprocessing

import org.junit.After
import org.junit.Before
import org.junit.Test
import grails.test.mixin.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ConfigPerProjectAndSeqType)
@Mock([Project, SeqType])
class ConfigPerProjectAndSeqTypeUnitTests {

    String configuration = "configuration"
    String filePath = "/tmp/otp/otp-test/tempConfigFile.txt"
    File configFile
    ConfigPerProjectAndSeqType validConfigPerProjectAndSeqType

    @Before
    void setUp() {
        configFile = new File(filePath)

        validConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
            project: new Project(),
            seqType: new SeqType(),
            configuration: configuration,
            )
        validConfigPerProjectAndSeqType.save()
    }

    @After
    void tearDown() {
        configFile.delete()
        validConfigPerProjectAndSeqType.delete()
    }

    void testSaveWithoutProject() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                seqType: new SeqType(),
                configuration: configuration,
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.project = new Project()
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithoutSeqType() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                configuration: configuration,
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.seqType = new SeqType()
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithoutConfig() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.configuration = configuration
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithEmptyConfig() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                configuration: "",
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.configuration = configuration
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithObsoleteDate() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                configuration: configuration,
                obsoleteDate: new Date(),
                )
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithReferenceToPreviousConfigWithoutObsolete() {
        ConfigPerProjectAndSeqType newConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                configuration: configuration,
                previousConfig: validConfigPerProjectAndSeqType,
                )
        assertFalse(newConfigPerProjectAndSeqType.validate())

        validConfigPerProjectAndSeqType.obsoleteDate = new Date()
        assertTrue(validConfigPerProjectAndSeqType.validate())
        assertTrue(newConfigPerProjectAndSeqType.validate())
    }

    void testWriteToFile() {
        validConfigPerProjectAndSeqType.writeToFile(configFile, true)
        assertEquals(configFile.text, configuration)
    }

    @Test
    void testWriteToFileNoAbsolutePath() {
        shouldFail AssertionError, {
            validConfigPerProjectAndSeqType.writeToFile(new File("tempConfigFile.txt"), false)
        }
    }

    @Test
    void testWriteToFileExistsAlready() {
        validConfigPerProjectAndSeqType.writeToFile(configFile, false)
        assertEquals(configFile.text, configuration)
        shouldFail RuntimeException, {
            validConfigPerProjectAndSeqType.writeToFile(configFile, false)
        }
    }
}
