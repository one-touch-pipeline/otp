package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 */

@TestFor(RoddyWorkflowConfig)
@Build([Project, Workflow])
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
        roddyWorkflowConfig.workflow = null

        TestCase.assertValidateError(roddyWorkflowConfig, 'workflow', 'nullable', null)
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
    void testUniqueConstraint() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig()

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project: roddyWorkflowConfig1.project,
                workflow: roddyWorkflowConfig1.workflow,
                configFilePath: roddyWorkflowConfig1.configFilePath,
                configVersion: DomainFactory.TEST_CONFIG_VERSION,
                ], false)

         assert !roddyWorkflowConfig2.validate()

    }

    @Test
    void testGetNameUsedInConfig_withConfigVersion_shouldBeCorrect() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        String expected = "${roddyWorkflowConfig.workflow.name}_${roddyWorkflowConfig.pluginVersion}_${roddyWorkflowConfig.configVersion}"

        assert expected == roddyWorkflowConfig.nameUsedInConfig
    }

    @Test
    void testGetNameUsedInConfig_withoutConfigVersion_shouldBeCorrect() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(configVersion: null)
        String expected = "${roddyWorkflowConfig.workflow.name}_${roddyWorkflowConfig.pluginVersion}"

        assert expected == roddyWorkflowConfig.nameUsedInConfig
    }



    @Test
    void tesValidateConfig_shouldBeValid() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.getNameUsedInConfig())

        roddyWorkflowConfig.validateConfig()
    }

    @Test
    void tesValidateConfig_shouldFailForMissingFile() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()

        TestCase.shouldFailWithMessage(AssertionError, 'File .* does not exist.*') {
            roddyWorkflowConfig.validateConfig()
        }
    }

    @Test
    void tesValidateConfig_shouldFailForFileName() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        File file = new File(configDir, "${HelperUtils.uniqueString}.xml")
        roddyWorkflowConfig.configFilePath = file.path
        assert file.createNewFile()

        TestCase.shouldFailWithMessage(AssertionError, '.*The file name .*does not match the pattern.*') {
            roddyWorkflowConfig.validateConfig()
        }
    }

    @Test
    void tesValidateConfig_shouldFailForPluginVersionInName() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.getNameUsedInConfig())
        roddyWorkflowConfig.pluginVersion = "plugin:invalid"

        TestCase.shouldFailWithMessageContaining(AssertionError, 'plugin:invalid') {
            roddyWorkflowConfig.validateConfig()
        }
    }

    @Test
    void tesValidateConfig_shouldFailForLabelInFile() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, 'label')

        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert configuration.@name == getNameUsedInConfig()') {
            roddyWorkflowConfig.validateConfig()
        }
    }



    void createXml(RoddyWorkflowConfig roddyWorkflowConfig, String label) {
        File file = new File(configDir, new File(roddyWorkflowConfig.configFilePath).name)
        roddyWorkflowConfig.configFilePath = file.path
        CreateFileHelper.createRoddyWorkflowConfig(file, label)
    }

}
