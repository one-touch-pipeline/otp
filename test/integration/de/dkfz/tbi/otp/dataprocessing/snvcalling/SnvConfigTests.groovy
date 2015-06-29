package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.utils.ExternalScript
import grails.validation.ValidationException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep.*
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class SnvConfigTests {

    Project project
    SeqType seqType
    ExternalScript externalScript



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

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        externalScript = SnvCallingInstanceTestData.createOrFindExternalScript()

        project = TestData.createProject(
                name: "projectName",
                dirName: "projectDirName",
                realmName: "realmName"
        )
        assert project.save(flush: true)

        seqType = new SeqType(
                name: "seqTypeName",
                libraryLayout: "seqTypeLibraryLayout",
                dirName: "seqTypeDirName"
        )
        assert seqType.save(flush: true)
    }

    @After
    void tearDown() {
        project = null
        seqType = null
        externalScript = null
    }

    @Test
    void testGetLatest() {
        SnvConfig config = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: "testConfig",
                externalScriptVersion: "v1",
        )
        assert config.save(flush:true)

        assertEquals(config, SnvConfig.getLatest(project, seqType))

        Project project2 = TestData.createProject(
                name: "project2Name",
                dirName: "project2DirName",
                realmName: "realmName"
                )
        assert project2.save()

        SeqType seqType2 = new SeqType(
                name: "seqType2Name",
                libraryLayout: "seqType2LibraryLayout",
                dirName: "seqType2DirName"
                )
        assert seqType2.save()

        SnvConfig config2 = new SnvConfig(
                project: project2,
                seqType: seqType2,
                configuration: "testConfig",
                externalScriptVersion: "v1",
                )
        assert config2.save(flush:true)
        assertEquals(config, SnvConfig.getLatest(project, seqType))
        assertEquals(config2, SnvConfig.getLatest(project2, seqType2))

        config.obsoleteDate = new Date()
        assert config.save(flush:true)

        config2.project = project
        config2.seqType = seqType
        config2.previousConfig = config
        assert config2.save(flush:true)

        assertEquals(config2, SnvConfig.getLatest(project, seqType))
    }


    @Test
    void testCreateFromFile_legalConfig() {
        final SnvConfig config = createFromFile(LEGAL_CONFIG)
        assertCorrectValues(config)
    }


    @Test
    void testCreateFromFile_NoScriptVersion_ShouldFail() {
        TestCase.shouldFail(ValidationException) {createFromFile(LEGAL_CONFIG, null)}
    }


    @Test
    void testCreateFromFile_UpdateConfig() {
        final SnvConfig firstConfig = createFromFile(LEGAL_CONFIG)
        assertCorrectValues(firstConfig)
        final SnvConfig secondConfig = createFromFile(LEGAL_CONFIG, "v1", firstConfig.project, firstConfig.seqType)
        assertCorrectValues(secondConfig)

        assert firstConfig.obsoleteDate
        assert secondConfig.previousConfig == firstConfig
    }

    @Test
    void testCreateFromFile_UpdateConfig_oldScriptIsDepricated() {
        final String version2 = "v2"
        final SnvConfig firstConfig = createFromFile(LEGAL_CONFIG)
        assertCorrectValues(firstConfig)
        externalScript.deprecatedDate = new Date()
        assert externalScript.save(flush: true, failOnError: true)
        SnvCallingInstanceTestData.createOrFindExternalScript(
                scriptVersion: version2,
                filePath: "/dev/null/otp-test/externalScript2.sh",
        )

        final SnvConfig secondConfig = createFromFile(LEGAL_CONFIG, version2, firstConfig.project, firstConfig.seqType)
        assertCorrectValues(secondConfig)

        assert firstConfig.obsoleteDate
        assert secondConfig.previousConfig == firstConfig
    }

    @Test
    void testEvaluate_legalConfig() {
        final SnvConfig config = createNormally(LEGAL_CONFIG)
        assert config.evaluate() == config
        assertCorrectValues(config)
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


    void withIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        createNormallyWithIllegalConfig(configuration, expectedExceptionMessage)
        createFromFileWithIllegalConfig(configuration, expectedExceptionMessage)
    }

    void createNormallyWithIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        final SnvConfig config = createNormally(configuration)
        assertEquals(expectedExceptionMessage, TestCase.shouldFail {
            config.evaluate()
        })
    }

    void createFromFileWithIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        assertEquals(expectedExceptionMessage, TestCase.shouldFail {
            createFromFile(configuration)
        })
    }

    SnvConfig createNormally(final String configuration) {
        final SnvConfig config = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: configuration,
                externalScriptVersion: "v1",
        )
        assert config.save()
        assertNotEvaluated(config)
        return config
    }


    SnvConfig createFromFile(final String configuration, String version = "v1", Project project = project, SeqType seqType = seqType) {
        tmpDir.create()

        File dir = tmpDir.newFolder("otp-test")
        assert dir.exists() || dir.mkdirs()

        File configFile = new File(dir, "configFile.txt")
        configFile << configuration

        try {
            final SnvConfig config = SnvConfig.createFromFile(project,
                    seqType, configFile, version)
            assertNotNull(config)
            assertEquals(configuration, config.configuration)
            return config
        } finally {
            configFile.delete()
        }
    }


    void assertNotEvaluated(final SnvConfig config) {
        assertEquals('Must call the evaluate() method first.'.toString(), TestCase.shouldFail(IllegalStateException, {
            config.getExecuteStepFlag(CALLING)
        }))
        assertEquals('Must call the evaluate() method first.'.toString(), TestCase.shouldFail(IllegalStateException, {
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
