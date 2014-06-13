package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.TestData
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test

// !! This class is only to test the abstract class ConfigPerProject
class ConfigPerProjectImpl extends ConfigPerProject { }

@TestFor(ConfigPerProjectImpl)
@Mock([Project])
class ConfigPerProjectUnitTests {

    String configuration = "configuration"

    @Test
    void testSaveWithoutProject_shouldFail() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                )
        TestCase.assertValidateError(configPerProject, 'project', 'nullable', null)

        configPerProject.project = TestData.createProject()
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithObsoleteDate() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                obsoleteDate: new Date(),
                )
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithReferenceToPreviousConfigWithoutObsolete_shouldFail() {
        ConfigPerProject validConfigPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
        )
        validConfigPerProject.save()

        ConfigPerProject newConfigPerProject = new ConfigPerProjectImpl(
                project: TestData.createProject(),
                previousConfig: validConfigPerProject,
                )
        TestCase.assertValidateError(newConfigPerProject, 'previousConfig', 'validator.invalid', validConfigPerProject)

        validConfigPerProject.obsoleteDate = new Date()
        assertTrue(validConfigPerProject.validate())
        assertTrue(newConfigPerProject.validate())
    }
}
