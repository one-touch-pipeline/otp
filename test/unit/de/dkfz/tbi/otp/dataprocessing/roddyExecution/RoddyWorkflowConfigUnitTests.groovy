package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import grails.test.mixin.web.*
import org.junit.*


@Mock([
        Individual,
        Pipeline,
        Project,
        ProjectCategory,
        RoddyWorkflowConfig,
        SeqType,
])
@TestMixin(ControllerUnitTestMixin)
public class RoddyWorkflowConfigUnitTests {

    File configDir
    File configFile

    @Before
    void setUp() {
        configDir = TestCase.createEmptyTestDirectory()
    }


    @After
    void tearDown() {
        TestCase.cleanTestDirectory()
    }

    @Test
    void testSaveRoddyWorkflowConfig_allFine() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        assert roddyWorkflowConfig.save(flush: true)
    }

    @Test
    void testSaveRoddyWorkflowConfig_NoWorkflow_ShouldFail() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.pipeline = null

        TestCase.assertValidateError(roddyWorkflowConfig, 'pipeline', 'nullable', null)
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigFilePathIsBlank_ShouldFail() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configFilePath = ''

        TestCase.assertValidateError(roddyWorkflowConfig, 'configFilePath', 'validator.invalid', '')
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigFileIsNotAbsolute_ShouldFail() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configFilePath = "tmp/"

        TestCase.assertValidateError(roddyWorkflowConfig, 'configFilePath', 'validator.invalid', 'tmp/')
    }

    @Test
    void testSaveRoddyWorkflowConfig_NoConfigVersion_ShouldBeValid() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = null

        assert roddyWorkflowConfig.validate()
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigVersionIsEmpty_ShouldBeInvalid() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = ''

        TestCase.assertValidateError(roddyWorkflowConfig, 'configVersion', 'blank', '')
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigVersionIsInvalid_ShouldBeInvalid() {
        String someInvalidVersion =  'invalidVersion'
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = someInvalidVersion

        TestCase.assertValidateError(roddyWorkflowConfig, 'configVersion', 'matches.invalid', someInvalidVersion)
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigVersionWithTwoDigits_ShouldBeValid() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = 'v12_34'

        assert roddyWorkflowConfig.validate()
    }

    @Test
    void testConfigVersionValidator_NoIndividual() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(
                obsoleteDate: new Date(),
        )

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
                configVersion: roddyWorkflowConfig1.configVersion,
                pluginVersion: roddyWorkflowConfig1.pluginVersion,
                ], false)

        TestCase.assertValidateError(roddyWorkflowConfig2, "configVersion", "validator.invalid", roddyWorkflowConfig2.configVersion)
    }

    @Test
    void testConfigVersionValidator_WithIndividualInOneConfig() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(
                individual: DomainFactory.createIndividual(),
                obsoleteDate: new Date(),
        )

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
                configVersion: roddyWorkflowConfig1.configVersion,
                pluginVersion: roddyWorkflowConfig1.pluginVersion,
        ], false)

        assert roddyWorkflowConfig2.validate()
    }

    @Test
    void testConfigVersionValidator_WithIndividualInBothConfigs() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(
                individual: DomainFactory.createIndividual(),
                obsoleteDate: new Date(),
        )

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
                pluginVersion: roddyWorkflowConfig1.pluginVersion,
                configVersion: roddyWorkflowConfig1.configVersion,
                individual: roddyWorkflowConfig1.individual,
        ], false)

        TestCase.assertValidateError(roddyWorkflowConfig2, "configVersion", "validator.invalid", roddyWorkflowConfig2.configVersion)
    }

    @Test
    void testConfigVersionValidator_WithDifferentIndividualInBothConfigs() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
                pluginVersion: roddyWorkflowConfig1.pluginVersion,
                configVersion: roddyWorkflowConfig1.configVersion,
                individual: DomainFactory.createIndividual(project: roddyWorkflowConfig1.project),
        ], false)

        assert roddyWorkflowConfig2.validate()
   }

    @Test
    void testObsoleteDateValidator_NoIndividual_invalid() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig()

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
        ], false)

        TestCase.assertValidateError(roddyWorkflowConfig2, "obsoleteDate", "validator.invalid", null)
    }

    @Test
    void testObsoleteDateValidator_NoIndividual_valid() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(obsoleteDate: new Date())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
        ], false)

        assert roddyWorkflowConfig2.validate()
    }

    @Test
    void testObsoleteDateValidator_WithIndividualInOneConfig_valid() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
        ], false)

        assert roddyWorkflowConfig2.validate()
    }

    @Test
    void testObsoleteDateValidator_WithIndividualInBothConfigs_invalid() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
                individual: roddyWorkflowConfig1.individual,
        ], false)

        TestCase.assertValidateError(roddyWorkflowConfig2, "obsoleteDate", "validator.invalid", null)
    }

    @Test
    void testObsoleteDateValidator_WithIndividualInBothConfigs_valid() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(obsoleteDate: new Date(), individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
                individual: roddyWorkflowConfig1.individual,
        ], false)

        assert roddyWorkflowConfig2.validate()
    }

    @Test
    void testObsoleteDateValidator_WithDifferentIndividualInBothConfigs_valid() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(obsoleteDate: new Date(), individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                seqType: roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
                individual: DomainFactory.createIndividual(project: roddyWorkflowConfig1.project),
        ], false)

        assert roddyWorkflowConfig2.validate()
    }

    @Test
    void testGetNameUsedInConfig_withConfigVersion_shouldBeCorrect() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        String expected = "${roddyWorkflowConfig.pipeline.name}_${roddyWorkflowConfig.seqType.roddyName}_${roddyWorkflowConfig.pluginVersion}_${roddyWorkflowConfig.configVersion}"

        assert expected == roddyWorkflowConfig.nameUsedInConfig
    }

    @Test
    void testGetNameUsedInConfig_withoutConfigVersion_shouldThrowException() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(configVersion: null)

        TestCase.shouldFailWithMessageContaining(AssertionError, 'Config version is not set') {
            roddyWorkflowConfig.nameUsedInConfig
        }
    }



    @Test
    void testValidateConfig_shouldBeValid() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.getNameUsedInConfig())

        roddyWorkflowConfig.validateConfig()
    }

    @Test
    void testValidateConfig_shouldFailForMissingFile() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()

        TestCase.shouldFailWithMessageContaining(AssertionError, 'not found.') {
            roddyWorkflowConfig.validateConfig()
        }
    }

    @Test
    void testValidateConfig_shouldFailForFileName() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        File file = new File(configDir, "${HelperUtils.uniqueString}.xml")
        roddyWorkflowConfig.configFilePath = file.path
        CreateFileHelper.createFile(file)

        TestCase.shouldFailWithMessage(AssertionError, '.*The file name .*does not match the pattern.*') {
            roddyWorkflowConfig.validateConfig()
        }
    }

    @Test
    void testValidateConfig_shouldFailForPluginVersionInName() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.getNameUsedInConfig())
        roddyWorkflowConfig.pluginVersion = "plugin:invalid"

        TestCase.shouldFailWithMessageContaining(AssertionError, 'plugin:invalid') {
            roddyWorkflowConfig.validateConfig()
        }
    }

    @Test
    void testValidateConfig_shouldFailForLabelInFile() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, 'label')

        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert configuration.@name == getNameUsedInConfig()') {
            roddyWorkflowConfig.validateConfig()
        }
    }

    @Test
    void testValidateConfig_withIndividual_PidInPath_shouldBeValid() {
        Individual individual = DomainFactory.createIndividual()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(individual: individual)
        configDir = new File(configDir, individual.pid)
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.getNameUsedInConfig())

        roddyWorkflowConfig.validateConfig()
    }


    @Test
    void testValidateConfig_withIndividual_PidNotInPath_shouldBeInvalid() {
        Individual individual = DomainFactory.createIndividual()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(individual: individual)
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.getNameUsedInConfig())

        TestCase.shouldFailWithMessageContaining(AssertionError, "assert configFilePath.contains(individual.pid)") {
            roddyWorkflowConfig.validateConfig()
        }
    }

    void createXml(RoddyWorkflowConfig roddyWorkflowConfig, String label) {
        File file = new File(configDir, new File(roddyWorkflowConfig.configFilePath).name)
        roddyWorkflowConfig.configFilePath = file.path
        CreateFileHelper.createRoddyWorkflowConfig(file, label)
    }

}
