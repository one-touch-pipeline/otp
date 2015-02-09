package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.TestData

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep.*

import org.junit.Test
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

@TestFor(SnvConfig)
@Mock([Project, SeqType])
class SnvConfigUnitTests extends TestCase {

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

    @Test
    void testIllegalExecuteFlagValue() {
        testWithIllegalConfig('RUN_CALLING=2', 'Illegal value for variable RUN_CALLING: 2')
    }

    @Test
    void testExecuteFlagMissing() {
        testWithIllegalConfig('RUN_CALLING=0', 'Illegal value for variable RUN_SNV_ANNOTATION: ')
    }

    @Test
    void testPreviousStepExecuteFlagIsNotSet() {
        testWithIllegalConfig(
                'RUN_CALLING=1\n' +
                'RUN_SNV_ANNOTATION=0\n' +
                'RUN_SNV_DEEPANNOTATION=0\n' +
                'RUN_FILTER_VCF=0',
                "Illegal config. ${SnvCallingStep.SNV_ANNOTATION} is configured not to be executed, but a previous step is.")
    }

    @Test
    void testDeepAnnotationRequiresAnnotation() {
        testWithIllegalConfig(
                'RUN_CALLING=0\n' +
                'RUN_SNV_ANNOTATION=0\n' +
                'RUN_SNV_DEEPANNOTATION=1\n' +
                'RUN_FILTER_VCF=1',
                "Illegal config, trying to do DeepAnnotation without the required Annotation step.")
    }

    @Test
    void testNoChromosomesSpecified() {
        testWithIllegalConfig(LEGAL_EXECUTE_FLAGS,'Illegal config. No chromosomes specified.')
    }

    @Test
    void testFailingConfigScript() {
        testWithIllegalConfig("LANG=C\n${LEGAL_CONFIG}\nmissing_dummy_command", 'Script failed with exit code 127. Error output:\nbash: line 7: missing_dummy_command: command not found\n')
    }

    @Test
    void testCreateFromFile_legalConfig() {
        final SnvConfig config = testCreateFromFile(LEGAL_CONFIG)
        assertCorrectValues(config)
    }


    @Test
    void testCreateFromFile_NoScriptVersion_ShouldFail() {
        shouldFail(AssertionError) {testCreateFromFile(LEGAL_CONFIG, null)}
    }


    @Test
    void testEvaluate_legalConfig() {
        final SnvConfig config = testCreateNormally(LEGAL_CONFIG)
        assert config.evaluate() == config
        assertCorrectValues(config)
    }

    void testWithIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        testCreateNormallyWithIllegalConfig(configuration, expectedExceptionMessage)
        testCreateFromFileWithIllegalConfig(configuration, expectedExceptionMessage)
    }

    void testCreateNormallyWithIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        final SnvConfig config = testCreateNormally(configuration)
        assertEquals(expectedExceptionMessage, shouldFail {
            config.evaluate()
        })
    }

    void testCreateFromFileWithIllegalConfig(final String configuration, final String expectedExceptionMessage) {
        assertEquals(expectedExceptionMessage, shouldFail {
            testCreateFromFile(configuration)
        })
    }

    SnvConfig testCreateNormally(final String configuration) {
        final SnvConfig config = new SnvConfig(
            project: new Project(),
            seqType: new SeqType(),
            configuration: configuration,
            externalScriptVersion: "v1",
        )
        assert config.save()
        assertNotEvaluated(config)
        return config
    }

    SnvConfig testCreateFromFile(final String configuration, String version = "v1") {
        File dir = new File("/tmp/otp/otp-unit-test")
        assert dir.exists() || dir.mkdirs()

        File configFile = new File(dir, "configFile.txt")
        configFile << configuration

        try {
            final SnvConfig config = SnvConfig.createFromFile(new Project(),
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
