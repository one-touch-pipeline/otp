package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test

import static de.dkfz.tbi.TestCase.createEmptyTestDirectory

@TestFor(SnvConfig)
@Build([ExternalScript, Project, SeqType])
@TestMixin(ControllerUnitTestMixin)
class SnvConfigUnitTests {

    static final String LEGAL_EXECUTE_FLAGS =
    "RUN_CALLING=0\n" +
    "RUN_SNV_ANNOTATION=1\n" +
    "RUN_SNV_DEEPANNOTATION=1\n" +
    "RUN_FILTER_VCF=1"

    ExternalScript externalScript
    SnvConfig validSnvConfig
    File configDir
    File configFile


    @Before
    void setUp() {
        externalScript = ExternalScript.build(scriptVersion: HelperUtils.uniqueString)

        validSnvConfig = new SnvConfig(
                project: DomainFactory.createProject(),
                seqType: new SeqType(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: externalScript.scriptVersion,
        )
        validSnvConfig.save()

        configDir = createEmptyTestDirectory()
        configFile = new File(configDir, 'tempConfigFile.txt')

    }


    @After
    void tearDown() {
        validSnvConfig.delete()
        configFile.delete()
        TestCase.cleanTestDirectory()
    }

    @Test
    void testSave_noScriptVersion_shouldNotValidate_shouldFail() {
        ConfigPerProject configPerProject = new SnvConfig(
                project: DomainFactory.createProject(),
                seqType: new SeqType(),
                configuration: LEGAL_EXECUTE_FLAGS,
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'nullable', null)

        configPerProject.externalScriptVersion = externalScript.scriptVersion
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSave_emptyScriptVersion_shouldNotValidate_shouldFail() {
        SnvConfig configPerProject = new SnvConfig(
                project: DomainFactory.createProject(),
                seqType: new SeqType(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: ""
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'blank', '')

        configPerProject.externalScriptVersion = externalScript.scriptVersion
        assertTrue(configPerProject.validate())
    }

    @Test
    void testExternalScriptExistsConstraint_NoExternalScript_ShouldFail() {
        ExternalScript.list().each {
            it.delete()
        }
        SnvConfig configPerProject = new SnvConfig(
                project: DomainFactory.createProject(),
                seqType: new SeqType(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: externalScript.scriptVersion
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'validator.invalid', externalScript.scriptVersion)
    }

    @Test
    void testObsoleteInstanceRefersToDeprecatedExternalScript_AllFine() {
        ExternalScript externalScript = ExternalScript.build(deprecatedDate: new Date())
        SnvConfig configPerProject = new SnvConfig(
                project: DomainFactory.createProject(),
                seqType: new SeqType(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: externalScript.scriptVersion,
                obsoleteDate: new Date()
        )
        assert configPerProject.validate()
    }

    @Test
    void testNonObsoleteInstanceRefersToDeprecatedExternalScript_ShouldNotValidate() {
        ExternalScript externalScript = ExternalScript.build(deprecatedDate: new Date())
        SnvConfig configPerProject = new SnvConfig(
                project: DomainFactory.createProject(),
                seqType: new SeqType(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: externalScript.scriptVersion
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'validator.invalid', externalScript.scriptVersion)
    }

    @Test
    void testSaveWithoutSeqType_shouldFail() {
        SnvConfig snvConfig = new SnvConfig(
                project: DomainFactory.createProject(),
                configuration: LEGAL_EXECUTE_FLAGS,
                externalScriptVersion: externalScript.scriptVersion,
                )
        assertFalse(snvConfig.validate())

        snvConfig.seqType = new SeqType()
        assertTrue(snvConfig.validate())
    }

    @Test
    void testSaveWithoutConfig_shouldFail() {
        SnvConfig snvConfig = new SnvConfig(
            project: DomainFactory.createProject(),
            seqType: new SeqType(),
            externalScriptVersion: externalScript.scriptVersion,
            )
        assertFalse(snvConfig.validate())

        snvConfig.configuration = LEGAL_EXECUTE_FLAGS
        assertTrue(snvConfig.validate())
    }

    @Test
    void testSaveWithEmptyConfig_shouldFail() {
        SnvConfig snvConfig = new SnvConfig(
            project: DomainFactory.createProject(),
            seqType: new SeqType(),
            configuration: "",
            externalScriptVersion: externalScript.scriptVersion,
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
