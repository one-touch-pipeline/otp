/*
 * Copyright 2011-2020 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@Mock([
        Individual,
        Pipeline,
        ProcessingPriority,
        Project,
        Realm,
        RoddyWorkflowConfig,
        SeqType,
])
@TestMixin(ControllerUnitTestMixin)
class RoddyWorkflowConfigUnitTests {

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

        TestCase.assertValidateError(roddyWorkflowConfig, 'configFilePath', 'blank', '')
    }

    @Test
    void testSaveRoddyWorkflowConfig_ConfigFileIsNotAbsolute_ShouldFail() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configFilePath = "tmp/"

        TestCase.assertValidateError(roddyWorkflowConfig, 'configFilePath', 'validator.absolute.path', 'tmp/')
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
                project       : roddyWorkflowConfig1.project,
                seqType       : roddyWorkflowConfig1.seqType,
                pipeline      : roddyWorkflowConfig1.pipeline,
                configVersion : roddyWorkflowConfig1.configVersion,
                programVersion: roddyWorkflowConfig1.programVersion,
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
                project       : roddyWorkflowConfig1.project,
                seqType       : roddyWorkflowConfig1.seqType,
                pipeline      : roddyWorkflowConfig1.pipeline,
                configVersion : roddyWorkflowConfig1.configVersion,
                programVersion: roddyWorkflowConfig1.programVersion,
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
                project       : roddyWorkflowConfig1.project,
                seqType       : roddyWorkflowConfig1.seqType,
                pipeline      : roddyWorkflowConfig1.pipeline,
                programVersion: roddyWorkflowConfig1.programVersion,
                configVersion : roddyWorkflowConfig1.configVersion,
                individual    : roddyWorkflowConfig1.individual,
        ], false)

        TestCase.assertValidateError(roddyWorkflowConfig2, "configVersion", "validator.invalid", roddyWorkflowConfig2.configVersion)
    }

    @Test
    void testConfigVersionValidator_WithDifferentIndividualInBothConfigs() {
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project       : roddyWorkflowConfig1.project,
                seqType       : roddyWorkflowConfig1.seqType,
                pipeline      : roddyWorkflowConfig1.pipeline,
                programVersion: roddyWorkflowConfig1.programVersion,
                configVersion : roddyWorkflowConfig1.configVersion,
                individual    : DomainFactory.createIndividual(project: roddyWorkflowConfig1.project),
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
        String expected = "${roddyWorkflowConfig.pipeline.name}_${roddyWorkflowConfig.seqType.roddyName}_${roddyWorkflowConfig.seqType.libraryLayout}_${roddyWorkflowConfig.programVersion}_${roddyWorkflowConfig.configVersion}"

        assert expected == roddyWorkflowConfig.nameUsedInConfig
    }

    @Test
    void testValidateConfig_shouldBeValid() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        RoddyWorkflowConfigService service = createService()

        service.validateConfig(roddyWorkflowConfig)
    }

    @Test
    void testValidateConfig_shouldFailForMissingFile() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        RoddyWorkflowConfigService service = createService()

        TestCase.shouldFailWithMessageContaining(AssertionError, 'on local filesystem is not accessible or does not exist.') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    @Test
    void testValidateConfig_shouldFailForFileName() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        File file = new File(configDir, "${HelperUtils.uniqueString}.xml")
        roddyWorkflowConfig.configFilePath = file.path
        CreateFileHelper.createFile(file)
        RoddyWorkflowConfigService service = createService()

        TestCase.shouldFailWithMessage(AssertionError, '.*The file name .*does not match the pattern.*') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    @Test
    void testValidateConfig_shouldFailForPluginVersionInName() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        roddyWorkflowConfig.programVersion = "plugin:invalid"
        RoddyWorkflowConfigService service = createService()

        TestCase.shouldFailWithMessageContaining(AssertionError, 'plugin:invalid') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    @Test
    void testValidateConfig_shouldFailForLabelInFile() {
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, 'label')
        RoddyWorkflowConfigService service = createService()

        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert configuration.@name == config.getNameUsedInConfig()') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    @Test
    void testValidateConfig_withIndividual_PidInPath_shouldBeValid() {
        Individual individual = DomainFactory.createIndividual()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(individual: individual)
        configDir = new File(configDir, individual.pid)
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        RoddyWorkflowConfigService service = createService()

        service.validateConfig(roddyWorkflowConfig)
    }


    @Test
    void testValidateConfig_withIndividual_PidNotInPath_shouldBeInvalid() {
        Individual individual = DomainFactory.createIndividual()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(individual: individual)
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        RoddyWorkflowConfigService service = createService()

        TestCase.shouldFailWithMessageContaining(AssertionError, "assert config.configFilePath.contains(config.individual.pid)") {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    static RoddyWorkflowConfigService createService() {
        RoddyWorkflowConfigService service = new RoddyWorkflowConfigService()
        service.fileSystemService = new TestFileSystemService()
        return service
    }

    void createXml(RoddyWorkflowConfig roddyWorkflowConfig, String label) {
        File file = new File(configDir, new File(roddyWorkflowConfig.configFilePath).name)
        roddyWorkflowConfig.configFilePath = file.path
        CreateFileHelper.createRoddyWorkflowConfig(file, label)
    }
}
