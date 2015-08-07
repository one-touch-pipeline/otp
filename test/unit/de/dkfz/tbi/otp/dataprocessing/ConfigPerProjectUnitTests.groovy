package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.utils.ExternalScript
import grails.buildtestdata.mixin.Build
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import static de.dkfz.tbi.TestCase.shouldFail

// !! This class is only to test the abstract class ConfigPerProject
class ConfigPerProjectImpl extends ConfigPerProject { }

@TestFor(ConfigPerProjectImpl)
@Mock([Project])
@Build([ExternalScript])
class ConfigPerProjectUnitTests {

    String configuration = "configuration"

    void testSaveWithoutProject_shouldFail() {
        ExternalScript externalScript = ExternalScript.build()
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                externalScriptVersion: externalScript.scriptVersion,
                )
        TestCase.assertValidateError(configPerProject, 'project', 'nullable', null)

        configPerProject.project = TestData.createProject()
        assertTrue(configPerProject.validate())
    }

    void testSaveWithObsoleteDate() {
        ExternalScript externalScript = ExternalScript.build()
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                obsoleteDate: new Date(),
                externalScriptVersion: externalScript.scriptVersion,
                )
        assertTrue(configPerProject.validate())
    }

    void testSave_noScriptVersion_shouldNotValidate_shouldFail() {
        ExternalScript externalScript = ExternalScript.build()
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'nullable', null)

        configPerProject.externalScriptVersion = externalScript.scriptVersion
        assertTrue(configPerProject.validate())
    }

    void testSave_emptyScriptVersion_shouldNotValidate_shouldFail() {
        ExternalScript externalScript = ExternalScript.build()
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: ""
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'blank', '')

        configPerProject.externalScriptVersion = externalScript.scriptVersion
        assertTrue(configPerProject.validate())
    }

    void testSaveWithReferenceToPreviousConfigWithoutObsolete_shouldFail() {
        ExternalScript externalScript = ExternalScript.build()
        ConfigPerProject validConfigPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: externalScript.scriptVersion
        )
        validConfigPerProject.save()

        ConfigPerProject newConfigPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                previousConfig: validConfigPerProject,
                externalScriptVersion: externalScript.scriptVersion,
                )
        TestCase.assertValidateError(newConfigPerProject, 'previousConfig', 'validator.invalid', validConfigPerProject)

        validConfigPerProject.obsoleteDate = new Date()
        assertTrue(validConfigPerProject.validate())
        assertTrue(newConfigPerProject.validate())
    }


    void testExternalScriptExistsConstraint_AllFine() {
        ExternalScript externalScript = ExternalScript.build()
        ConfigPerProjectImpl configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: externalScript.scriptVersion
        )
        assert configPerProject.validate()
    }


    void testExternalScriptExistsConstraint_NoExternalScript_ShouldFail() {
        ConfigPerProjectImpl configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: "v1"
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'validator.invalid', 'v1')
    }

    void testObsolateInstanceRefersToDeprecatedExternalScript_AllFine() {
        ExternalScript externalScript = ExternalScript.build(deprecatedDate: new Date())
        ConfigPerProjectImpl configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: externalScript.scriptVersion,
                obsoleteDate: new Date()
        )
        assert configPerProject.validate()
    }

    void testNonObsolateInstanceRefersToDeprecatedExternalScript_ShouldNotValidate() {
        ExternalScript externalScript = ExternalScript.build(deprecatedDate: new Date())
        ConfigPerProjectImpl configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: externalScript.scriptVersion
        )
        TestCase.assertValidateError(configPerProject, 'externalScriptVersion', 'validator.invalid', externalScript.scriptVersion)
    }

}
