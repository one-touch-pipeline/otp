package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.ConfigPerProjectAndSeqType.Purpose
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ConfigPerProjectAndSeqType)
@Mock([Project, SeqType])
class ConfigPerProjectAndSeqTypeUnitTests {

	String configuration = "configuration"

	void testSaveWithoutProject() {
		ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
				seqType: new SeqType(),
				configuration: configuration,
				purpose: Purpose.SNV
				)
		assertFalse(configPerProjectAndSeqType.validate())

		configPerProjectAndSeqType.project = new Project()
		assertTrue(configPerProjectAndSeqType.validate())
	}

	void testSaveWithoutSeqType() {
		ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
				project: new Project(),
				configuration: configuration,
				purpose: Purpose.SNV
				)
		assertFalse(configPerProjectAndSeqType.validate())

		configPerProjectAndSeqType.seqType = new SeqType()
		assertTrue(configPerProjectAndSeqType.validate())
	}

	void testSaveWithoutConfig() {
		ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
				project: new Project(),
				seqType: new SeqType(),
				purpose: Purpose.SNV
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
				purpose: Purpose.SNV
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
				purpose: Purpose.SNV
				)
		assertTrue(configPerProjectAndSeqType.validate())
	}

	void testSaveWithReferenceToPreviousConfigWithoutObsolete() {
		ConfigPerProjectAndSeqType oldConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
				project: new Project(),
				seqType: new SeqType(),
				configuration: configuration,
				purpose: Purpose.SNV
				)
		assertTrue(oldConfigPerProjectAndSeqType.validate())


		ConfigPerProjectAndSeqType newConfigPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
				project: new Project(),
				seqType: new SeqType(),
				configuration: configuration,
				previousConfig: oldConfigPerProjectAndSeqType,
				purpose: Purpose.SNV
				)
		assertFalse(newConfigPerProjectAndSeqType.validate())

		oldConfigPerProjectAndSeqType.obsoleteDate = new Date()
		assertTrue(oldConfigPerProjectAndSeqType.validate())
		assertTrue(newConfigPerProjectAndSeqType.validate())
	}

	void testSaveWithoutPurpose() {
		ConfigPerProjectAndSeqType configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
				project: new Project(),
				seqType: new SeqType(),
				configuration: configuration
				)
		assertFalse(configPerProjectAndSeqType.validate())

		configPerProjectAndSeqType.purpose = Purpose.SNV
		assertTrue(configPerProjectAndSeqType.validate())
	}


	void testCreateFromFile() {
		String configuration = "header\nconfigration\ntest"
		File dir = new File("/tmp/otp/otp-unit-test")
		assert dir.exists() || dir.mkdirs()

		File configFile = new File(dir, "configFile.txt")
		configFile << configuration

		ConfigPerProjectAndSeqType configPerProjectAndSeqType = ConfigPerProjectAndSeqType.createFromFile(new Project(),
				new SeqType(), Purpose.SNV, configFile)
		assertNotNull(configPerProjectAndSeqType)
		assertEquals(configuration, configPerProjectAndSeqType.configuration)
		configFile.delete()
	}
}
