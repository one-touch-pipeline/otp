package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

// !! This class is only to test the abstract class ConfigPerProjectAndSeqType
class ConfigPerProjectAndSeqTypeImpl extends ConfigPerProjectAndSeqType { }

@TestFor(ConfigPerProjectAndSeqTypeImpl)
@Mock([Pipeline, Project, ProjectCategory, Realm, SeqType])
class ConfigPerProjectAndSeqTypeUnitTests {


    @Test
    void testSaveWithoutProject_shouldFail() {
        ConfigPerProjectAndSeqType configPerProject = new ConfigPerProjectAndSeqTypeImpl(
                pipeline: DomainFactory.createIndelPipelineLazy(),
                seqType: DomainFactory.createSeqType(),
        )
        TestCase.assertValidateError(configPerProject, 'project', 'nullable', null)

        configPerProject.project = DomainFactory.createProject()
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithObsoleteDate() {
        ConfigPerProjectAndSeqType configPerProject = new ConfigPerProjectAndSeqTypeImpl(
                project: DomainFactory.createProject(),
                obsoleteDate: new Date(),
                pipeline: DomainFactory.createIndelPipelineLazy(),
                seqType: DomainFactory.createSeqType(),
        )
        assertTrue(configPerProject.validate())
    }

    @Test
    void testSaveWithReferenceToPreviousConfigWithoutObsolete_shouldFail() {
        ConfigPerProjectAndSeqType validConfigPerProject = new ConfigPerProjectAndSeqTypeImpl(
                project: DomainFactory.createProject(),
                seqType: DomainFactory.createSeqType(),
                pipeline: DomainFactory.createIndelPipelineLazy(),
        )
        validConfigPerProject.save()

        ConfigPerProjectAndSeqType newConfigPerProject = new ConfigPerProjectAndSeqTypeImpl(
                project: DomainFactory.createProject(),
                seqType: DomainFactory.createSeqType(),
                pipeline: DomainFactory.createIndelPipelineLazy(),
                previousConfig: validConfigPerProject,
        )
        TestCase.assertValidateError(newConfigPerProject, 'previousConfig', 'validator.invalid', validConfigPerProject)

        validConfigPerProject.obsoleteDate = new Date()
        assertTrue(validConfigPerProject.validate())
        assertTrue(newConfigPerProject.validate())
    }
}
