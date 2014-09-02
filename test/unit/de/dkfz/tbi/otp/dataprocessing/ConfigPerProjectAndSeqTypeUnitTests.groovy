package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ConfigPerProjectAndSeqType)
@Mock([Project, SeqType])
class ConfigPerProjectAndSeqTypeUnitTests {

    String configuration = "configuration"

    void testSaveWithoutProject() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                seqType: new SeqType(),
                configuration: configuration,
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.project = new Project()
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithoutSeqType() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                configuration: configuration,
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.seqType = new SeqType()
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithoutConfig() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.configuration = configuration
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithEmptyConfig() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                configuration: "",
                )
        assertFalse(configPerProjectAndSeqType.validate())

        configPerProjectAndSeqType.configuration = configuration
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithObsoleteDate() {
        ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                configuration: configuration,
                obsoleteDate: new Date(),
                )
        assertTrue(configPerProjectAndSeqType.validate())
    }

    void testSaveWithReferenceToPreviousConfigWithoutObsolete() {
        ConfigPerProjectAndSeqType oldConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                configuration: configuration,
                )
        assertTrue(oldConfigPerProjectAndSeqType.validate())


        ConfigPerProjectAndSeqType newConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: new Project(),
                seqType: new SeqType(),
                configuration: configuration,
                previousConfig: oldConfigPerProjectAndSeqType,
                )
        assertFalse(newConfigPerProjectAndSeqType.validate())

        oldConfigPerProjectAndSeqType.obsoleteDate = new Date()
        assertTrue(oldConfigPerProjectAndSeqType.validate())
        assertTrue(newConfigPerProjectAndSeqType.validate())
    }
}
