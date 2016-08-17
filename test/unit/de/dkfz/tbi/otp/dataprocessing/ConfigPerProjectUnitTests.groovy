package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ProjectCategory
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test

// !! This class is only to test the abstract class ConfigPerProject
class ConfigPerProjectImpl extends ConfigPerProject { }

@TestFor(ConfigPerProjectImpl)
@Mock([Pipeline, Project, ProjectCategory,])
class ConfigPerProjectUnitTests {

    String configuration = "configuration"

    @Test
    void testSaveWithoutProject_shouldFail() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                pipeline: DomainFactory.createOtpSnvPipelineLazy(),
                )
        TestCase.assertValidateError(configPerProject, 'project', 'nullable', null)

        configPerProject.project = DomainFactory.createProject()
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithObsoleteDate() {
        ConfigPerProject configPerProject = new ConfigPerProjectImpl(
                project: DomainFactory.createProject(),
                obsoleteDate: new Date(),
                pipeline: DomainFactory.createOtpSnvPipelineLazy(),
                )
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithReferenceToPreviousConfigWithoutObsolete_shouldFail() {
        ConfigPerProject validConfigPerProject = new ConfigPerProjectImpl(
                project: DomainFactory.createProject(),
                pipeline: DomainFactory.createOtpSnvPipelineLazy(),
        )
        validConfigPerProject.save()

        ConfigPerProject newConfigPerProject = new ConfigPerProjectImpl(
                project: DomainFactory.createProject(),
                pipeline: DomainFactory.createOtpSnvPipelineLazy(),
                previousConfig: validConfigPerProject,
                )
        TestCase.assertValidateError(newConfigPerProject, 'previousConfig', 'validator.invalid', validConfigPerProject)

        validConfigPerProject.obsoleteDate = new Date()
        assertTrue(validConfigPerProject.validate())
        assertTrue(newConfigPerProject.validate())
    }
}
