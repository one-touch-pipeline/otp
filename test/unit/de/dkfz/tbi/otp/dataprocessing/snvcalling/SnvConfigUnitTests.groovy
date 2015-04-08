package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.TestData

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.test.mixin.*
import static de.dkfz.tbi.TestCase.createEmptyTestDirectory

@TestFor(SnvConfig)
@Mock([Project, SeqType])
class SnvConfigUnitTests {

    static final String LEGAL_EXECUTE_FLAGS =
    "RUN_CALLING=0\n" +
    "RUN_SNV_ANNOTATION=1\n" +
    "RUN_SNV_DEEPANNOTATION=1\n" +
    "RUN_FILTER_VCF=1"
    static final String LEGAL_CHROMOSOME_INDICES =
            "CHROMOSOME_INDICES=( {8..11} A BC )"
    static final String LEGAL_CONFIG =
            "${LEGAL_EXECUTE_FLAGS}\n" +
            "${LEGAL_CHROMOSOME_INDICES}"


    SnvConfig validSnvConfig
    File configDir
    File configFile


    @Before
    void setUp() {
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
    void testIllegalExecuteFlagValue() {
        withIllegalConfig('RUN_CALLING=2', 'Illegal value for variable RUN_CALLING: 2')
    }

    @Test
    void testExecuteFlagMissing() {
        withIllegalConfig('RUN_CALLING=0', 'Illegal value for variable RUN_SNV_ANNOTATION: ')
    }

    @Test
    void testPreviousStepExecuteFlagIsNotSet() {
        withIllegalConfig(
                'RUN_CALLING=1\n' +
                'RUN_SNV_ANNOTATION=0\n' +
                'RUN_SNV_DEEPANNOTATION=0\n' +
                'RUN_FILTER_VCF=0',
                "Illegal config. ${SnvCallingStep.SNV_ANNOTATION} is configured not to be executed, but a previous step is.")
    }

    @Test
    void testDeepAnnotationRequiresAnnotation() {
        withIllegalConfig(
                'RUN_CALLING=0\n' +
                'RUN_SNV_ANNOTATION=0\n' +
                'RUN_SNV_DEEPANNOTATION=1\n' +
                'RUN_FILTER_VCF=1',
                "Illegal config, trying to do DeepAnnotation without the required Annotation step.")
    }

    @Test
    void testNoChromosomesSpecified() {
        withIllegalConfig(LEGAL_EXECUTE_FLAGS,'Illegal config. No chromosomes specified.')
    }

    @Test
    void testFailingConfigScript() {
        withIllegalConfig("LANG=C\n${LEGAL_CONFIG}\nmissing_dummy_command", 'Script failed with exit code 127. Error output:\nbash: line 7: missing_dummy_command: command not found\n')
    }

    @Test
    void testCreateFromFile_legalConfig() {
        final SnvConfig config = createFromFile(LEGAL_CONFIG)
        assertCorrectValues(config)
    }


    @Test
    void testCreateFromFile_NoScriptVersion_ShouldFail() {
        shouldFail(AssertionError) {createFromFile(LEGAL_CONFIG, null)}
    }


    @Test
    void testEvaluate_legalConfig() {
        final SnvConfig config = createNormally(LEGAL_CONFIG)
        assert config.evaluate() == config
        assertCorrectValues(config)
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

    void withIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        createNormallyWithIllegalConfig(configuration, expectedExceptionMessage)
        createFromFileWithIllegalConfig(configuration, expectedExceptionMessage)
    }

    void createNormallyWithIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        final SnvConfig config = createNormally(configuration)
        assertEquals(expectedExceptionMessage, shouldFail {
            config.evaluate()
        })
    }

    void createFromFileWithIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        assertEquals(expectedExceptionMessage, shouldFail {
            createFromFile(configuration)
        })
    }

    SnvConfig createNormally(final String configuration) {
        final SnvConfig config = new SnvConfig(
            project: TestData.createProject(),
            seqType: new SeqType(),
            configuration: configuration,
            externalScriptVersion: "v1",
        )
        assert config.save()
        assertNotEvaluated(config)
        return config
    }

    SnvConfig createFromFile(final String configuration, String version = "v1") {
        File dir = new File("/tmp/otp/otp-unit-test")
        assert dir.exists() || dir.mkdirs()

        File configFile = new File(dir, "configFile.txt")
        configFile << configuration

        try {
            final SnvConfig config = SnvConfig.createFromFile(TestData.createProject(),
                    new SeqType(), configFile, version)
            assertNotNull(config)
            assertEquals(configuration, config.configuration)
            return config
        } finally {
            configFile.delete()
        }
    }

    void assertNotEvaluated(final SnvConfig config) {
        assertEquals('Must call the evaluate() method first.'.toString(), shouldFail(IllegalStateException, {
            config.getExecuteStepFlag(CALLING)
        }))
        assertEquals('Must call the evaluate() method first.'.toString(), shouldFail(IllegalStateException, {
            config.chromosomeNames
        }))
    }

    void assertCorrectValues(final SnvConfig config) {
        assert !config.getExecuteStepFlag(CALLING)
        assert config.getExecuteStepFlag(SNV_ANNOTATION)
        assert config.getExecuteStepFlag(SNV_DEEPANNOTATION)
        assert config.getExecuteStepFlag(FILTER_VCF)
        assert config.chromosomeNames == ['8', '9', '10', '11', 'A', 'BC']
    }
}
