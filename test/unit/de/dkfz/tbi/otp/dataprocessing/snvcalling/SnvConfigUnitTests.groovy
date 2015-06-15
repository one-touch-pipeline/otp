package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.utils.ExternalScript
import org.junit.After
import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.test.mixin.*
import static de.dkfz.tbi.TestCase.createEmptyTestDirectory

@TestFor(SnvConfig)
@Mock([ExternalScript, Project, SeqType])
class SnvConfigUnitTests {

    static final String LEGAL_EXECUTE_FLAGS =
    "RUN_CALLING=0\n" +
    "RUN_SNV_ANNOTATION=1\n" +
    "RUN_SNV_DEEPANNOTATION=1\n" +
    "RUN_FILTER_VCF=1"

    SnvConfig validSnvConfig
    File configDir
    File configFile


    @Before
    void setUp() {
        SnvCallingInstanceTestData.createOrFindExternalScript()

        validSnvConfig = new SnvConfig(
                project: TestData.createProject(),
                seqType: new SeqType(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: "v1",
        )
        validSnvConfig.save()

        configDir = createEmptyTestDirectory()
        configFile = new File(configDir, 'tempConfigFile.txt')

    }


    @After
    void tearDown() {
        validSnvConfig.delete()
        configFile.delete()
        configDir.deleteDir()

    }

    @Test
    void testSaveWithoutSeqType_shouldFail() {
        SnvConfig snvConfig = new SnvConfig(
                project: TestData.createProject(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: "v1",
                )
        assertFalse(snvConfig.validate())

        snvConfig.seqType = new SeqType()
        assertTrue(snvConfig.validate())
    }

    void testSaveWithoutConfig_shouldFail() {
        SnvConfig snvConfig = new SnvConfig(
            project: TestData.createProject(),
            seqType: new SeqType(),
            externalScriptVersion: "v1",
            )
        assertFalse(snvConfig.validate())

        snvConfig.configuration = LEGAL_EXECUTE_FLAGS
        assertTrue(snvConfig.validate())
    }

    void testSaveWithEmptyConfig_shouldFail() {
        SnvConfig snvConfig = new SnvConfig(
            project: TestData.createProject(),
            seqType: new SeqType(),
            configuration: "",
            externalScriptVersion: "v1",
        )
        assertFalse(snvConfig.validate())

        snvConfig.configuration = LEGAL_EXECUTE_FLAGS
        assertTrue(snvConfig.validate())
    }


    @Test
    void testWriteToFile_WhenFileDoesNotExistAndOverwriteIsSet_ShouldCreateFile() {
        validSnvConfig.writeToFile(configFile, true)
        assertEquals(configFile.text, LEGAL_EXECUTE_FLAGS)

    }

    @Test
    void testWriteToFile_WhenFileDoesExistAndOverwriteIsSet_ShouldCreateFile() {
        configFile << 'something different'
        validSnvConfig.writeToFile(configFile, true)
        assertEquals(configFile.text, LEGAL_EXECUTE_FLAGS)

    }

    @Test
    void testWriteToFileNoAbsolutePath() {
        shouldFail AssertionError, {
            validSnvConfig.writeToFile(new File("tempConfigFile.txt"), false)
        }
    }

    @Test
    void testWriteToFileExistsAlready() {
        validSnvConfig.writeToFile(configFile, false)
        assertEquals(configFile.text, LEGAL_EXECUTE_FLAGS)
        shouldFail RuntimeException, {
            validSnvConfig.writeToFile(configFile, false)
        }
    }
}
