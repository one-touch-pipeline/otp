package de.dkfz.tbi.otp.dataprocessing

import org.junit.Test
import grails.test.mixin.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.TestCase.createEmptyTestDirectory

// !! This class is only to test the abstract class ConfigPerProject
class ConfigPerProjectImpl extends ConfigPerProject { }

@TestFor(ConfigPerProjectImpl)
@Mock([Project])
class ConfigPerProjectUnitTests {

    String configuration = "configuration"

    void testSaveWithoutProject_shouldFail() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                externalScriptVersion: "v1",
                )
        assertFalse(configPerProject.validate())

        configPerProject.project = TestData.createProject()
        assertTrue(configPerProject.validate())
    }

    void testSaveWithObsoleteDate() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                obsoleteDate: new Date(),
                externalScriptVersion: "v1",
                )
        assertTrue(configPerProject.validate())
    }

    void testSave_noScriptVersion_shouldNotValidate_shouldFail() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
        )
        assertFalse(configPerProject.validate())

        configPerProject.externalScriptVersion = "v1"
        assertTrue(configPerProject.validate())
    }

    void testSave_emptyScriptVersion_shouldNotValidate_shouldFail() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: ""
        )
        assertFalse(configPerProject.validate())

        configPerProject.externalScriptVersion = "v1"
        assertTrue(configPerProject.validate())
    }

    void testSaveWithReferenceToPreviousConfigWithoutObsolete_shouldFail() {
        ConfigPerProject validConfigPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                externalScriptVersion: "v1"
        )
        validConfigPerProject.save()

        ConfigPerProject newConfigPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                previousConfig: validConfigPerProject,
                externalScriptVersion: "v1",
                )
        assertFalse(newConfigPerProject.validate())

        validConfigPerProject.obsoleteDate = new Date()
        assertTrue(validConfigPerProject.validate())
        assertTrue(newConfigPerProject.validate())
    }

}
