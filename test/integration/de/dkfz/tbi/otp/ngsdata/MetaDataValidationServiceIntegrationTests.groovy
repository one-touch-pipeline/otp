package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

class MetaDataValidationServiceIntegrationTests {

    MetaDataValidationService metaDataValidationService
    Run run

    @Before
    void setUp() {
        SeqPlatform seqPlatform = SeqPlatform.build(
                    name: "PlatformName",
                    model: "platformModel",
        )

        SeqCenter seqCenter = SeqCenter.build(
                    name: "seqCenterName",
                    dirName: "seqCenterDirName",
        )

        run = Run.build(
                    name: "runName",
                    seqCenter: seqCenter,
                    seqPlatform: seqPlatform,
                    storageRealm: Run.StorageRealm.DKFZ,
        )
    }

    @After
    void tearDown() {
        run = null
    }

    @Test
    void testValidateMetaDataEntry_LIB_PREP_KIT_valid() {
        DataFile dataFile = DataFile.build()

        MetaDataKey key = MetaDataKey.build(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        MetaDataEntry entry = MetaDataEntry.build(
                value: SeqTypeNames.EXOME.seqTypeName,
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        key = MetaDataKey.build(name: MetaDataColumn.LIB_PREP_KIT.toString())

        String libraryPreparationKitName = "Agilent SureSelect V3"
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build(name: libraryPreparationKitName)

        entry = MetaDataEntry.build(
                value: libraryPreparationKitName,
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        assertTrue metaDataValidationService.validateMetaDataEntry(run, entry)
    }

    @Test
    void testValidateMetaDataEntry_LIB_PREP_KIT_invalid() {
        DataFile dataFile = DataFile.build()

        MetaDataKey key = MetaDataKey.build(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        MetaDataEntry entry = MetaDataEntry.build(
                value: SeqTypeNames.EXOME.seqTypeName,
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        key = MetaDataKey.build(name: MetaDataColumn.LIB_PREP_KIT.toString())

        String libraryPreparationKitName = "Agilent SureSelect V3"
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build(name: libraryPreparationKitName)

        entry = MetaDataEntry.build(
                value: "some enrichement Kit name not present in the database",
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        assertFalse metaDataValidationService.validateMetaDataEntry(run, entry)
    }

    @Test
    void testValidateMetaDataEntry_ANTIBODY_TARGET_valid() {
        DataFile dataFile = DataFile.build()

        MetaDataKey key = MetaDataKey.build(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        MetaDataEntry entry = MetaDataEntry.build(
                value: SeqTypeNames.CHIP_SEQ.seqTypeName,
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        key = MetaDataKey.build(name: MetaDataColumn.ANTIBODY_TARGET.toString())

        String antibodyTargetName = "lala3LALA"
        AntibodyTarget antibodyTarget = AntibodyTarget.build(name: antibodyTargetName)

        entry = MetaDataEntry.build(
                value: antibodyTargetName,
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        assertTrue metaDataValidationService.validateMetaDataEntry(run, entry)

        assertNull ChangeLog.findByRowId(entry.id)
    }

    @Test
    void testValidateMetaDataEntry_ANTIBODY_TARGETwithWrongCase_valid() {
        DataFile dataFile = DataFile.build()

        MetaDataKey key = MetaDataKey.build(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        MetaDataEntry entry = MetaDataEntry.build(
                value: SeqTypeNames.CHIP_SEQ.seqTypeName,
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        key = MetaDataKey.build(name: MetaDataColumn.ANTIBODY_TARGET.toString())

        String antibodyTargetName = "lala3LALA"
        AntibodyTarget antibodyTarget = AntibodyTarget.build(
                name: antibodyTargetName.toUpperCase()
        )

        entry = MetaDataEntry.build(
                value: antibodyTargetName.toLowerCase(),
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        assertTrue metaDataValidationService.validateMetaDataEntry(run, entry)

        def changeLog = ChangeLog.findByRowId(entry.id)
        assertNotNull changeLog
        assertEquals changeLog.columnName, "value"
        assertEquals changeLog.fromValue, antibodyTargetName.toLowerCase()
        assertEquals changeLog.toValue, antibodyTargetName.toUpperCase()
        assertEquals changeLog.comment, "fixed wrong upper/lower case"
        assertEquals changeLog.source, ChangeLog.Source.SYSTEM
    }

    @Test
    void testValidateMetaDataEntry_ANTIBODY_TARGET_invalid() {
        DataFile dataFile = DataFile.build()

        MetaDataKey key = MetaDataKey.build(name: MetaDataColumn.SEQUENCING_TYPE.toString())
        MetaDataEntry entry = MetaDataEntry.build(
                value: SeqTypeNames.CHIP_SEQ.seqTypeName,
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        key = MetaDataKey.build(name: MetaDataColumn.ANTIBODY_TARGET.toString())

        String antibodyTargetName = "lala3LALA"
        AntibodyTarget antibodyTarget = AntibodyTarget.build(name: antibodyTargetName)

        entry = MetaDataEntry.build(
                value: "some antibody target name not present in the database",
                dataFile: dataFile,
                key: key,
                source: MetaDataEntry.Source.MANUAL,
        )

        assertFalse metaDataValidationService.validateMetaDataEntry(run, entry)

        assertNull ChangeLog.findByRowId(entry.id)
    }
}
