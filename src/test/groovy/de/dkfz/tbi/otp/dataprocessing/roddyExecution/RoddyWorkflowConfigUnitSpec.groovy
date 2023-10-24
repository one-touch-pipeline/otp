/*
 * Copyright 2011-2023 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class RoddyWorkflowConfigUnitSpec extends Specification implements DataTest {

    File configDir
    File configFile

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                Individual,
                Pipeline,
                ProcessingPriority,
                Project,
                RoddyWorkflowConfig,
                SeqType,
        ]
    }

    void setup() {
        configDir = TestCase.createEmptyTestDirectory()
    }

    void testSaveRoddyWorkflowConfig_allFine() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()

        when:
        roddyWorkflowConfig.save(flush: true)

        then:
        noExceptionThrown()
    }

    void testSaveRoddyWorkflowConfig_NoWorkflow_ShouldFail() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.pipeline = null

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig, 'pipeline', 'nullable', null)
    }

    void testSaveRoddyWorkflowConfig_ConfigFilePathIsBlank_ShouldFail() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configFilePath = ''

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig, 'configFilePath', 'blank', '')
    }

    void testSaveRoddyWorkflowConfig_ConfigFileIsNotAbsolute_ShouldFail() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configFilePath = "tmp/"

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig, 'configFilePath', 'validator.absolute.path', 'tmp/')
    }

    void testSaveRoddyWorkflowConfig_NoConfigVersion_ShouldBeValid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = null

        expect:
        roddyWorkflowConfig.validate()
    }

    void testSaveRoddyWorkflowConfig_ConfigVersionIsEmpty_ShouldBeInvalid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = ''

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig, 'configVersion', 'blank', '')
    }

    void testSaveRoddyWorkflowConfig_ConfigVersionIsInvalid_ShouldBeInvalid() {
        given:
        String someInvalidVersion = 'invalidVersion'
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = someInvalidVersion

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig, 'configVersion', 'matches.invalid', someInvalidVersion)
    }

    void testSaveRoddyWorkflowConfig_ConfigVersionWithTwoDigits_ShouldBeValid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        roddyWorkflowConfig.configVersion = 'v12_34'

        expect:
        roddyWorkflowConfig.validate()
    }

    void testConfigVersionValidator_NoIndividual() {
        given:
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

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig2, "configVersion", "validator.invalid", roddyWorkflowConfig2.configVersion)
    }

    void testConfigVersionValidator_WithIndividualInOneConfig() {
        given:
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

        expect:
        roddyWorkflowConfig2.validate()
    }

    void testConfigVersionValidator_WithIndividualInBothConfigs() {
        given:
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

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig2, "configVersion", "validator.invalid", roddyWorkflowConfig2.configVersion)
    }

    void testConfigVersionValidator_WithDifferentIndividualInBothConfigs() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project       : roddyWorkflowConfig1.project,
                seqType       : roddyWorkflowConfig1.seqType,
                pipeline      : roddyWorkflowConfig1.pipeline,
                programVersion: roddyWorkflowConfig1.programVersion,
                configVersion : roddyWorkflowConfig1.configVersion,
                individual    : DomainFactory.createIndividual(project: roddyWorkflowConfig1.project),
        ], false)

        expect:
        roddyWorkflowConfig2.validate()
    }

    void testObsoleteDateValidator_NoIndividual_invalid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig()

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project : roddyWorkflowConfig1.project,
                seqType : roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
        ], false)

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig2, "obsoleteDate", "validator.invalid", null)
    }

    void testObsoleteDateValidator_NoIndividual_valid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(obsoleteDate: new Date())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project : roddyWorkflowConfig1.project,
                seqType : roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
        ], false)

        expect:
        roddyWorkflowConfig2.validate()
    }

    void testObsoleteDateValidator_WithIndividualInOneConfig_valid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project : roddyWorkflowConfig1.project,
                seqType : roddyWorkflowConfig1.seqType,
                pipeline: roddyWorkflowConfig1.pipeline,
        ], false)

        expect:
        roddyWorkflowConfig2.validate()
    }

    void testObsoleteDateValidator_WithIndividualInBothConfigs_invalid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project   : roddyWorkflowConfig1.project,
                seqType   : roddyWorkflowConfig1.seqType,
                pipeline  : roddyWorkflowConfig1.pipeline,
                individual: roddyWorkflowConfig1.individual,
        ], false)

        expect:
        TestCase.assertValidateError(roddyWorkflowConfig2, "obsoleteDate", "validator.invalid", null)
    }

    void testObsoleteDateValidator_WithIndividualInBothConfigs_valid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(obsoleteDate: new Date(), individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project   : roddyWorkflowConfig1.project,
                seqType   : roddyWorkflowConfig1.seqType,
                pipeline  : roddyWorkflowConfig1.pipeline,
                individual: roddyWorkflowConfig1.individual,
        ], false)

        expect:
        roddyWorkflowConfig2.validate()
    }

    void testObsoleteDateValidator_WithDifferentIndividualInBothConfigs_valid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(obsoleteDate: new Date(), individual: DomainFactory.createIndividual())

        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig([
                project   : roddyWorkflowConfig1.project,
                seqType   : roddyWorkflowConfig1.seqType,
                pipeline  : roddyWorkflowConfig1.pipeline,
                individual: DomainFactory.createIndividual(project: roddyWorkflowConfig1.project),
        ], false)

        expect:
        roddyWorkflowConfig2.validate()
    }

    void testGetNameUsedInConfig_withConfigVersion_shouldBeCorrect() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        String expected = "${roddyWorkflowConfig.pipeline.name}_${roddyWorkflowConfig.seqType.roddyName}_${roddyWorkflowConfig.seqType.libraryLayout}_${roddyWorkflowConfig.programVersion}_${roddyWorkflowConfig.configVersion}"

        expect:
        expected == roddyWorkflowConfig.nameUsedInConfig
    }

    void testValidateConfig_shouldBeValid() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        RoddyWorkflowConfigService service = createService()

        expect:
        service.validateConfig(roddyWorkflowConfig)
    }

    void testValidateConfig_shouldFailForMissingFile() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        RoddyWorkflowConfigService service = createService()

        expect:
        TestCase.shouldFailWithMessageContaining(AssertionError, 'on local filesystem is not accessible or does not exist.') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    void testValidateConfig_shouldFailForFileName() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        File file = new File(configDir, "${HelperUtils.uniqueString}.xml")
        roddyWorkflowConfig.configFilePath = file.path
        CreateFileHelper.createFile(file)
        RoddyWorkflowConfigService service = createService()

        expect:
        TestCase.shouldFailWithMessage(AssertionError, '.*The file name .*does not match the pattern.*') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    void testValidateConfig_shouldFailForPluginVersionInName() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        roddyWorkflowConfig.programVersion = "plugin:invalid"
        RoddyWorkflowConfigService service = createService()

        expect:
        TestCase.shouldFailWithMessageContaining(AssertionError, 'plugin:invalid') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    void testValidateConfig_shouldFailForLabelInFile() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        createXml(roddyWorkflowConfig, 'label')
        RoddyWorkflowConfigService service = createService()

        expect:
        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert configuration.@name == config.nameUsedInConfig') {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    void testValidateConfig_withIndividual_PidInPath_shouldBeValid() {
        given:
        Individual individual = DomainFactory.createIndividual()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(individual: individual)
        configDir = new File(configDir, individual.pid)
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        RoddyWorkflowConfigService service = createService()

        expect:
        service.validateConfig(roddyWorkflowConfig)
    }

    void testValidateConfig_withIndividual_PidNotInPath_shouldBeInvalid() {
        given:
        Individual individual = DomainFactory.createIndividual()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(individual: individual)
        createXml(roddyWorkflowConfig, roddyWorkflowConfig.nameUsedInConfig)
        RoddyWorkflowConfigService service = createService()

        expect:
        TestCase.shouldFailWithMessageContaining(AssertionError, "assert config.configFilePath.contains(config.individual.pid)") {
            service.validateConfig(roddyWorkflowConfig)
        }
    }

    private RoddyWorkflowConfigService createService() {
        RoddyWorkflowConfigService service = new RoddyWorkflowConfigService()
        service.fileSystemService = new TestFileSystemService()
        service.configService = Mock(ConfigService)
        service.fileService = new FileService()
        service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        return service
    }

    private void createXml(RoddyWorkflowConfig roddyWorkflowConfig, String label) {
        File file = new File(configDir, new File(roddyWorkflowConfig.configFilePath).name)
        roddyWorkflowConfig.configFilePath = file.path
        CreateFileHelper.createRoddyWorkflowConfig(file, label)
    }
}
