package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

class MetaDataValidationServiceIntegrationTests {

    MetaDataValidationService metaDataValidationService
    Run run

    @Before
    void setUp() {
        SeqPlatform seqPlatform = new SeqPlatform(
                    name: "PlatformName",
                    model: "platformModel")
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))

        SeqCenter seqCenter = new SeqCenter(
                    name: "seqCenterName",
                    dirName: "seqCenterDirName")
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        run = new Run(
                    name: "runName",
                    seqCenter: seqCenter,
                    seqPlatform: seqPlatform,
                    storageRealm: Run.StorageRealm.DKFZ)
        assertNotNull(run.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        run = null
    }

    @Test
    void testValidateMetaDataEntry_LIB_PREP_KIT_valid() {
        DataFile dataFile = new DataFile()
        assertTrue(dataFile.validate())
        assertNotNull(dataFile.save(flush: true))

        MetaDataKey key = new MetaDataKey(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))
        MetaDataEntry entry = new MetaDataEntry(value: SeqTypeNames.EXOME.seqTypeName, dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        key = new MetaDataKey(name: MetaDataColumn.LIB_PREP_KIT.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))

        String libraryPreparationKitName = "Agilent SureSelect V3"
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(name: libraryPreparationKitName)
        assertNotNull(libraryPreparationKit.save(flush: true))

        entry = new MetaDataEntry(value: libraryPreparationKitName, dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        assertTrue metaDataValidationService.validateMetaDataEntry(run, entry)
    }

    @Test
    void testValidateMetaDataEntry_LIB_PREP_KIT_invalid() {
        DataFile dataFile = new DataFile()
        assertTrue(dataFile.validate())
        assertNotNull(dataFile.save(flush: true))

        MetaDataKey key = new MetaDataKey(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))
        MetaDataEntry entry = new MetaDataEntry(value: SeqTypeNames.EXOME.seqTypeName, dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        key = new MetaDataKey(name: MetaDataColumn.LIB_PREP_KIT.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))

        String libraryPreparationKitName = "Agilent SureSelect V3"
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(name: libraryPreparationKitName)
        assertNotNull(libraryPreparationKit.save(flush: true))

        entry = new MetaDataEntry(value: "some enrichement Kit name not present in the database", dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        assertFalse metaDataValidationService.validateMetaDataEntry(run, entry)
    }

    @Test
    void testValidateMetaDataEntry_ANTIBODY_TARGET_valid() {
        DataFile dataFile = new DataFile()
        assertTrue(dataFile.validate())
        assertNotNull(dataFile.save(flush: true))

        MetaDataKey key = new MetaDataKey(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))
        MetaDataEntry entry = new MetaDataEntry(value: SeqTypeNames.CHIP_SEQ.seqTypeName, dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        key = new MetaDataKey(name: MetaDataColumn.ANTIBODY_TARGET.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))

        String antibodyTargetName = "lala3LALA"
        AntibodyTarget antibodyTarget = new AntibodyTarget(name: antibodyTargetName)
        assertNotNull(antibodyTarget.save(flush: true))

        entry = new MetaDataEntry(value: antibodyTargetName, dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        assertTrue metaDataValidationService.validateMetaDataEntry(run, entry)
    }

    @Test
    void testValidateMetaDataEntry_ANTIBODY_TARGET_invalid() {
        DataFile dataFile = new DataFile()
        assertTrue(dataFile.validate())
        assertNotNull(dataFile.save(flush: true))

        SeqTypeNames.EXOME.seqTypeName

        MetaDataKey key = new MetaDataKey(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))
        MetaDataEntry entry = new MetaDataEntry(value: SeqTypeNames.CHIP_SEQ.seqTypeName, dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        key = new MetaDataKey(name: MetaDataColumn.ANTIBODY_TARGET.toString())
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))

        String antibodyTargetName = "lala3LALA"
        AntibodyTarget antibodyTarget = new AntibodyTarget(name: antibodyTargetName)
        assertNotNull(antibodyTarget.save(flush: true))

        entry = new MetaDataEntry(value: "some antibody target name not present in the database", dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)

        assertFalse metaDataValidationService.validateMetaDataEntry(run, entry)
    }
}
