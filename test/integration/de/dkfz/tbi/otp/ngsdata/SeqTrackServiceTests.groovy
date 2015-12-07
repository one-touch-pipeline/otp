package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.FastqSet
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.MetaDataEntry.Source
import de.dkfz.tbi.otp.ngsdata.MetaDataEntry.Status
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.TestCase

class SeqTrackServiceTests extends AbstractIntegrationTest {

    SeqTrackService seqTrackService

    TestData testData

    File dataPath
    File mdPath

    static final String ANTIBODY_TARGET_IDENTIFIER = "AntibodyTargetIdentifier123"
    static final String ANTIBODY_IDENTIFIER = "AntibodyIdentifier123"
    static final String LIBRARY_PREPARATION_KIT_NAME_VALID = "Valid kit name"
    static final String LIBRARY_PREPARATION_KIT_SYNONYM_VALID = "Valid kit synonym"
    static final String LIBRARY_PREPARATION_KIT_NAME_INVALID = "Invalid kit name"
    static final String ILSE_ID = "1234"
    static final String LANE_NR = "2"

    // the String "UNKNOWN" is used instead of the enum, because that is how it appears in external input files
    final String UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE = "UNKNOWN"

    @Before
    void setUp() {
        dataPath = TestCase.getUniqueNonExistentPath()
        mdPath = TestCase.getUniqueNonExistentPath()
        testData = new TestData()
    }

    @After
    void tearDown() {
        TestCase.cleanTestDirectory()
        testData = null
    }

    @Ignore
    @Test
    void testBuildSequenceTracks() {
        Run run = new Run()
        assertFalse(run.validate())
        run.name = "testRun"
        run.complete = false
        SeqCenter seqCenter = new SeqCenter(name: "testSeqCenter", dirName: "testDir")
        assert(seqCenter.save())
        run.seqCenter = seqCenter
        SeqPlatform seqPlatform = SeqPlatform.build()
        run.seqPlatform = seqPlatform
        assert(run.save())
        // does not proceed as no DataFile is set with the run associated
        seqTrackService.buildSequenceTracks(run.id)
        FileType fileType = new FileType(type: FileType.Type.ALIGNMENT)
        assert(fileType.save())
        SeqType seqType = new SeqType(name: "testSeqType", libraryLayout: "testLibraryLayout", dirName: "testDir")
        assert(seqType.save())
        Sample sample = new Sample(type: Sample.Type.TUMOR, subType: null)
        Realm realm = new Realm(name: "test", rootPath: "/", webHost: "http://test.me", host: "127.0.0.1", port: 12345, unixUser: "test", timeout: 100, pbsOptions: "")
        assertNotNull(realm.save())
        Project project = TestData.createProject(name: "testProject", dirName: "testDir", host: "dkfz", realm: realm)
        assert(project.save())
        Individual individual = new Individual(pid: "testPid", mockPid: "testMockPid", mockFullName: "testMockFullName", type: Individual.Type.POOL, project: project)
        assert(individual.save())
        sample.individual = individual
        assert(sample.save())
        SoftwareTool softwareTool = new SoftwareTool(programName: "testProgram", type: SoftwareTool.Type.BASECALLING, programVersion: "1")
        assert(softwareTool.save())
        SeqTrack seqTrack = new SeqTrack(laneId: "testLaneId", pipelineVersion: softwareTool, run: run, seqType: seqType, seqPlatform: seqPlatform, sample: sample)
        assert(seqTrack.save())
        DataFile dataFile = new DataFile(fileName: "dataFile1", pathName: "testPath", metaDataValid: false, run: run, fileType: fileType, seqTrack: seqTrack)
        assert(dataFile.save())
        // returns without real processing because DataFile.metaDataValid is false
        seqTrackService.buildSequenceTracks(run.id)
        dataFile.metaDataValid = true
        // returns without real processing because FileType.Type is not SEQUENCE
        seqTrackService.buildSequenceTracks(run.id)
        fileType.type = FileType.Type.SEQUENCE
        assert(fileType.save())
        // no MetaDataEntry
        shouldFail(LaneNotDefinedException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        Run otherRun = new Run()
        otherRun.name = "otherTestRun"
        otherRun.complete = false
        otherRun.seqCenter = seqCenter
        otherRun.seqPlatform = seqPlatform
        assert(otherRun.save())
        DataFile differentlyAssociatedDataFile = new DataFile(fileName: "dataFile1", pathName: "testPath", metaDataValid: false, run: otherRun, fileType: fileType ,seqTrack: seqTrack)
        assert(differentlyAssociatedDataFile.save())
        MetaDataKey metaDataKey = new MetaDataKey(name: "testKey")
        assert(metaDataKey.save())
        MetaDataEntry metaDataEntry = new MetaDataEntry(value: "testValue", dataFile: differentlyAssociatedDataFile, key: metaDataKey, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry.save())
        // MetaDataEntry not associated with run handed over
        shouldFail(LaneNotDefinedException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        metaDataEntry.dataFile = dataFile
        // no appropriate key specified
        shouldFail(LaneNotDefinedException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        MetaDataKey metaDataKey2 = new MetaDataKey(name: "LANE_NO")
        assert(metaDataKey2.save())
        MetaDataEntry metaDataEntry1 = new MetaDataEntry(value: "testEntry1", dataFile: dataFile, key: metaDataKey2, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry1.save(flush: true))
        // No laneDataFile
        shouldFail(ProcessingException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        FileType sequenceFileType = new FileType(type: FileType.Type.SEQUENCE)
        assert(sequenceFileType.save())
        dataFile.fileType = sequenceFileType
        assert(dataFile.save(flush: true))
        // no entry for key: SAMPLE_ID
        shouldFail(ProcessingException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        MetaDataKey metaDataKey3 = new MetaDataKey(name: "SAMPLE_ID")
        assert(metaDataKey3.save())
        MetaDataEntry metaDataEntry2 = new MetaDataEntry(value: "testEntry2", dataFile: dataFile, key: metaDataKey3, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry2.save(flush: true))
        MetaDataKey metaDataKey4 = new MetaDataKey(name: "SEQUENCING_TYPE")
        assert(metaDataKey4.save())
        MetaDataEntry metaDataEntry3 = new MetaDataEntry(value: "testEntry3", dataFile: dataFile, key: metaDataKey4, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry3.save(flush: true))
        MetaDataKey metaDataKey5 = new MetaDataKey(name: "LIBRARY_LAYOUT")
        assert(metaDataKey5.save())
        MetaDataEntry metaDataEntry4 = new MetaDataEntry(value: "testEntry4", dataFile: dataFile, key: metaDataKey5, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry4.save(flush: true))
        MetaDataKey metaDataKey6 = new MetaDataKey(name: "PIPELINE_VERSION")
        assert(metaDataKey6.save())
        MetaDataEntry metaDataEntry5 = new MetaDataEntry(value: "testEntry5", dataFile: dataFile, key: metaDataKey6, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry5.save(flush: true))
        SampleIdentifier sampleIdentifier = new SampleIdentifier(name: "testEntry2", sample: sample)
        assert(sampleIdentifier.save())
        SampleIdentifier sampleIdentifier2 = new SampleIdentifier(name: "testEntry3", sample: sample)
        assert(sampleIdentifier2.save())
        // sequencing type not defiened
        shouldFail(SeqTypeNotDefinedException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        SeqType seqType2 = new SeqType(name: "testEntry3", libraryLayout: "testEntry4", dirName: "testDir")
        assert(seqType2.save())
        // seqTrack could not be validated
        shouldFail(ProcessingException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        // SoftwareToolIdentifier missing
        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier(name: "testEntry5", softwareTool: softwareTool)
        assert(softwareToolIdentifier.save())
        // no entry for key: INSERT_SIZE
        shouldFail(ProcessingException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        MetaDataKey metaDataKey7 = new MetaDataKey(name: "INSERT_SIZE")
        assert(metaDataKey7.save())
        MetaDataEntry metaDataEntry6 = new MetaDataEntry(value: "testEntry6", dataFile: dataFile, key: metaDataKey7, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry6.save())
        // no entry for key: READ_COUNT
        shouldFail(ProcessingException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        MetaDataKey metaDataKey8 = new MetaDataKey(name: "READ_COUNT")
        assert(metaDataKey8.save())
        MetaDataEntry metaDataEntry7 = new MetaDataEntry(value: "testEntry7", dataFile: dataFile, key: metaDataKey8, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry7.save())
        // no entry for key: BASE_COUNT
        shouldFail(ProcessingException) {
            seqTrackService.buildSequenceTracks(run.id)
        }
        MetaDataKey metaDataKey9 = new MetaDataKey(name: "BASE_COUNT")
        assert(metaDataKey9.save())
        MetaDataEntry metaDataEntry8 = new MetaDataEntry(value: "testEntry8", dataFile: dataFile, key: metaDataKey9, source: MetaDataEntry.Source.SYSTEM)
        assert(metaDataEntry8.save())
        seqTrackService.buildSequenceTracks(run.id)
    }

    @Test
    void testAssertConsistentLibraryPreparationKitConsistent() {
        MetaDataKey metaDataKey = new MetaDataKey(name: "LIB_PREP_KIT")
        assertNotNull(metaDataKey.save())
        String libraryPreparationKitSomething = "something"
        DataFile dataFileR1 = new DataFile(fileName: "1_ACTGTG_L005_R1_complete_filtered.fastq.gz")
        assertNotNull(dataFileR1.save())
        MetaDataEntry metaDataEntry = new MetaDataEntry(value: libraryPreparationKitSomething, dataFile: dataFileR1, key: metaDataKey, source: MetaDataEntry.Source.SYSTEM)
        assertNotNull(metaDataEntry.save())
        DataFile dataFileR2 = new DataFile(fileName: "1_ACTGTG_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFileR2.save())
        metaDataEntry = new MetaDataEntry(value: libraryPreparationKitSomething, dataFile: dataFileR2, key: metaDataKey, source: MetaDataEntry.Source.SYSTEM)
        assertNotNull(metaDataEntry.save())

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        seqTrackService.assertConsistentLibraryPreparationKit(dataFiles)
    }

    @Test
    void testAssertConsistentLibraryPreparationKitNotConsistent() {
        MetaDataKey metaDataKey = new MetaDataKey(name: "LIB_PREP_KIT")
        assertNotNull(metaDataKey.save())

        String libraryPreparationKitSomething = "something"
        DataFile dataFileR1 = new DataFile(fileName: "1_ATCACG_L005_R1_complete_filtered.fastq.gz")
        assertNotNull(dataFileR1.save())
        MetaDataEntry metaDataEntry = new MetaDataEntry(value: libraryPreparationKitSomething, dataFile: dataFileR1, key: metaDataKey, source: MetaDataEntry.Source.SYSTEM)
        assertNotNull(metaDataEntry.save())

        String libraryPreparationKitDifferent = "different"
        DataFile dataFileR2 = new DataFile(fileName: "1_ATCACG_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFileR2.save())
        metaDataEntry = new MetaDataEntry(value: libraryPreparationKitDifferent, dataFile: dataFileR2, key: metaDataKey, source: MetaDataEntry.Source.SYSTEM)
        assertNotNull(metaDataEntry.save())

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        shouldFail(ProcessingException) {
            seqTrackService.assertConsistentLibraryPreparationKit(dataFiles)
        }
    }

    @Test
    void testAssertConsistentLibraryPreparationKitNoLibPrepKitKey() {
        DataFile dataFileR1 = new DataFile(fileName: "1_ACTGTG_L005_R1_complete_filtered.fastq.gz")
        assertNotNull(dataFileR1.save())
        DataFile dataFileR2 = new DataFile(fileName: "1_ACTGTG_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFileR2.save())

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        seqTrackService.assertConsistentLibraryPreparationKit(dataFiles)
    }

    @Test
    void testAssertConsistentLibraryPreparationKitNoLibPrepKitValue() {
        MetaDataKey metaDataKey = new MetaDataKey(name: "LIB_PREP_KIT")
        assertNotNull(metaDataKey.save())

        DataFile dataFileR1 = new DataFile(fileName: "1_ACTGTG_L005_R1_complete_filtered.fastq.gz")
        assertNotNull(dataFileR1.save())
        DataFile dataFileR2 = new DataFile(fileName: "1_ACTGTG_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFileR2.save())

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        seqTrackService.assertConsistentLibraryPreparationKit(dataFiles)
    }


    Run createDataForBuildFastqSeqTrack(String key, String valueRead1, String valueRead2) {
        SeqType.build(name: SeqTypeNames.EXOME.seqTypeName, libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        LibraryPreparationKit.build(name: "LIB_PREP_KIT")
        SoftwareToolIdentifier.build(name: "PIPELINE_VERSION")
        SampleIdentifier sampleIdentifier = SampleIdentifier.build(name: "SAMPLE_ID")
        Project project = sampleIdentifier.sample.project
        project.alignmentDeciderBeanName = 'noAlignmentDecider'
        assert project.save(failOnError: true)
        Run run = Run.build(seqCenter: SeqCenter.buildLazy(name: "DKFZ"))
        RunSegment runSegment = RunSegment.build(run: run)
        Map data = [
                LANE_NO: LANE_NR,
                SAMPLE_ID: "SAMPLE_ID",
                SEQUENCING_TYPE: SeqTypeNames.EXOME.seqTypeName,
                LIB_PREP_KIT: "LIB_PREP_KIT",
                PIPELINE_VERSION: "PIPELINE_VERSION",
                ILSE_NO: ILSE_ID,
                LIBRARY_LAYOUT: SeqType.LIBRARYLAYOUT_PAIRED,
                INSERT_SIZE: "10000",
                BASE_COUNT: "100000",
                READ_COUNT: "1000",
        ]
        createAndSaveDataFileAndMetaDataEntry(data + [(key): valueRead1], run, runSegment)
        createAndSaveDataFileAndMetaDataEntry(data + [(key): valueRead2], run, runSegment)
        return run
    }


    @Test
    void testBuildFastqSeqTrack_SeqTrackCanBeBuild() {
        Run run = createDataForBuildFastqSeqTrack("SAMPLE_ID", "SAMPLE_ID", "SAMPLE_ID")
        SeqTrack seqTrack = seqTrackService.buildFastqSeqTrack(run, LANE_NR)
        assert seqTrack.run == run
        assert seqTrack.ilseId == ILSE_ID
        assert seqTrack.laneId == LANE_NR
    }

    @Test
    void testBuildFastqSeqTrack_NoDataFileFound_ThrowException() {
        Run run = createDataForBuildFastqSeqTrack("SAMPLE_ID", "SAMPLE_ID", "SAMPLE_ID")

        assert (TestCase.shouldFail(ProcessingException) {
            seqTrackService.buildFastqSeqTrack(Run.build(), LANE_NR)
        }).contains("No laneDataFiles found.")
    }

    @Test
    void testBuildFastqSeqTrack_SamplesAreDifferent_ThrowException() {
        Run run = createDataForBuildFastqSeqTrack("SAMPLE_ID", "SAMPLE_ID_1", "SAMPLE_ID_2")
        SampleIdentifier.build(name: "SAMPLE_ID_1")
        SampleIdentifier.build(name: "SAMPLE_ID_2")
        shouldFail(SampleInconsistentException) {seqTrackService.buildFastqSeqTrack(run, LANE_NR)}
    }

    @Test
    void testBuildFastqSeqTrack_SeqTypesAreDifferent_ThrowException() {
        Run run = createDataForBuildFastqSeqTrack("SEQUENCING_TYPE", "SEQUENCING_TYPE_1", "SEQUENCING_TYPE_2")
        SeqType.build(name: "SEQUENCING_TYPE_1", libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        SeqType.build(name: "SEQUENCING_TYPE_2", libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        shouldFail(MetaDataInconsistentException) {seqTrackService.buildFastqSeqTrack(run, LANE_NR)}

    }

    @Test
    void testBuildFastqSeqTrack_ExomeSeqTrack_LibraryPreparationKitsAreDifferent_ThrowException() {
        Run run = createDataForBuildFastqSeqTrack("LIB_PREP_KIT", "LIB_PREP_KIT_1", "LIB_PREP_KIT_2")
        assert (TestCase.shouldFail(ProcessingException) {
            seqTrackService.buildFastqSeqTrack(run, LANE_NR)
        }).contains("Not using the same LIB_PREP_KIT")

    }

    @Test
    void testBuildFastqSeqTrack_SoftwareToolsAreDifferent_ThrowException() {
        Run run = createDataForBuildFastqSeqTrack("PIPELINE_VERSION", "PIPELINE_VERSION_1", "PIPELINE_VERSION_2")
        SoftwareToolIdentifier.build(name: "PIPELINE_VERSION_1")
        SoftwareToolIdentifier.build(name: "PIPELINE_VERSION_2")
        shouldFail(MetaDataInconsistentException) {seqTrackService.buildFastqSeqTrack(run, LANE_NR)}
    }

    @Test
    void testBuildFastqSeqTrack_IlseIdsAreDifferent_ThrowDifferent() {
        Run run = createDataForBuildFastqSeqTrack("ILSE_NO", "ILSE_NO_1", "ILSE_NO_2")
        assert (TestCase.shouldFail(ProcessingException) {
            seqTrackService.buildFastqSeqTrack(run, LANE_NR)
        }).contains("Not using the same ILSE_NO")
    }


    @Test
    void testAssertConsistentWithinSeqTrack_NotEntryInDataBaseForMetaDataKey() {
        MetaDataKey metaDataKey = new MetaDataKey(name: MetaDataColumn.SEQUENCING_TYPE.name())
        assertNotNull(metaDataKey.save())

        DataFile dataFileR1 = new DataFile(fileName: "1_ACTGTG_L005_R1_complete_filtered.fastq.gz")
        assertNotNull(dataFileR1.save())

        DataFile dataFileR2 = new DataFile(fileName: "1_ACTGTG_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFileR2.save())

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        seqTrackService.assertConsistentWithinSeqTrack(dataFiles, MetaDataColumn.SEQUENCING_TYPE) == null
    }

    @Test
    void testAssertConsistentWithinSeqTrack_ValuesAreNotConsistent_ShouldFail() {
        String sequencingType = "sequencingType"
        DataFile dataFileR1 = createAndSaveDataFileAndMetaDataEntry([(MetaDataColumn.SEQUENCING_TYPE.name()): sequencingType])

        String differentSequencingType = "differentSequencingType"
        DataFile dataFileR2 = createAndSaveDataFileAndMetaDataEntry([(MetaDataColumn.SEQUENCING_TYPE.name()): differentSequencingType])

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        shouldFail(ProcessingException) {
            seqTrackService.assertConsistentWithinSeqTrack(dataFiles, MetaDataColumn.SEQUENCING_TYPE)
        }
    }

    @Test
    void testAssertConsistentWithinSeqTrack_ValuesAreConsistent() {
        String sequencingType = "sequencingType"

        DataFile dataFileR1 = createAndSaveDataFileAndMetaDataEntry([(MetaDataColumn.SEQUENCING_TYPE.name()): sequencingType])
        DataFile dataFileR2 = createAndSaveDataFileAndMetaDataEntry([(MetaDataColumn.SEQUENCING_TYPE.name()): sequencingType])

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        assert sequencingType == seqTrackService.assertConsistentWithinSeqTrack(dataFiles, MetaDataColumn.SEQUENCING_TYPE)
    }

    @Test
    void testAssertAndReturnConcistentIlseId_ValuesAreConsistent() {
        String ilseId = "1234"

        DataFile dataFileR1 = createAndSaveDataFileAndMetaDataEntry([(MetaDataColumn.ILSE_NO.name()): ilseId])
        DataFile dataFileR2 = createAndSaveDataFileAndMetaDataEntry([(MetaDataColumn.ILSE_NO.name()): ilseId])

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        assert ilseId == seqTrackService.assertAndReturnConcistentIlseId(dataFiles)
    }

    @Test
    void testCreateSeqTrack_WithIlseId() {
        String ilseId = "1234"
        Map data = createData()
        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                data.dataFile,
                data.run,
                data.sample,
                data.seqType,
                "1",
                data.softwareTool,
                ilseId
        )
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.class)
        assert seqTrack.ilseId == ilseId
    }



    @Test
    void testCreateSeqTrack_shouldReturnSeqTrack_NoMetadataForLibraryPreparationKit() {
        Map data = createData()

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.class)
    }

    @Test
    void testCreateSeqTrack_shouldReturnSeqTrack_WithUsingKitByName() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, LIBRARY_PREPARATION_KIT_NAME_VALID)

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.KNOWN, seqTrack.kitInfoReliability)
        assertEquals(data.libraryPreparationKit, seqTrack.libraryPreparationKit)
    }

    @Test
    void testCreateSeqTrack_shouldReturnSeqTrack_WithKitUsingBySynonym() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, LIBRARY_PREPARATION_KIT_SYNONYM_VALID)

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.KNOWN, seqTrack.kitInfoReliability)
        assertEquals(data.libraryPreparationKit, seqTrack.libraryPreparationKit)
    }

    @Test
    void testCreateSeqTrack_ShouldReturnExomeSeqTrack_WithSpecialValueUnknownForExome() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE)
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(ExomeSeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.UNKNOWN_VERIFIED, seqTrack.kitInfoReliability)
        assertNull(seqTrack.libraryPreparationKit)
    }

    @Test
    void testCreateSeqTrack_shouldFailForNotAllowedSpecialValueUnknownForNonExome() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE)

        TestCase.shouldFail(AssertionError.class) {
            seqTrackService.createSeqTrack(
                            data.dataFile,
                            data.run,
                            data.sample,
                            data.seqType,
                            "1",
                            data.softwareTool
                            )
        }
    }

    @Test
    void testCreateSeqTrack_ShouldReturnSeqTrack_WithEmptyKitValueForNonExome() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, '')

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrack.kitInfoReliability)
        assertNull(seqTrack.libraryPreparationKit)
    }

    @Test
    void testCreateSeqTrack_shouldFailForEmptyKitValueForExome() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, '')
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        TestCase.shouldFail(AssertionError.class) {
            seqTrackService.createSeqTrack(
                            data.dataFile,
                            data.run,
                            data.sample,
                            data.seqType,
                            "1",
                            data.softwareTool
                            )
        }
    }



    @Test
    void testCreateSeqTrack_shouldFailForNotAllowedSpecialValueInMetaData_UnknownUnverified() {
        //value UNKNOWN_UNVERIFIED can not given in meta data, so this value should be handled like an unknown value
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "UNKNOWN_UNVERIFIED")

        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.createSeqTrack(
                            data.dataFile,
                            data.run,
                            data.sample,
                            data.seqType,
                            "1",
                            data.softwareTool
                            )
        }
    }

    @Test
    void testCreateSeqTrack_shouldFailForUnknownKitValue() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, LIBRARY_PREPARATION_KIT_NAME_INVALID)

        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.createSeqTrack(
                            data.dataFile,
                            data.run,
                            data.sample,
                            data.seqType,
                            "1",
                            data.softwareTool
                            )
        }
    }

    @Test
    void testCreateSeqTrack_ShouldFailForMissingSample() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.LIB_PREP_KIT, LIBRARY_PREPARATION_KIT_NAME_VALID)

        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.createSeqTrack(
                            data.dataFile,
                            data.run,
                            null,
                            data.seqType,
                            "1",
                            data.softwareTool
                            )
        }
    }

    @Test
    void testCreateSeqTrack_ShouldReturnSeqTrack_NoLibraryPreparationKitColumnKey() {
        Map data = createData()

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrack.kitInfoReliability)
        assertNull(seqTrack.libraryPreparationKit)
    }

    @Test
    void testCreateSeqTrack_ShouldReturnSeqTrack_NoLibraryPreparationKitColumn() {
        Map data = createData()
        assertNotNull(data.seqType.save([flush: true]))
        MetaDataKey metaDataKey = MetaDataKey.build(
                        name: MetaDataColumn.LIB_PREP_KIT.name()
                        )

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrack.kitInfoReliability)
        assertNull(seqTrack.libraryPreparationKit)
    }

    @Test
    void testCreateSeqTrackChipSeqAntibodyTargetWithUnknownValue() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.ANTIBODY_TARGET, "unknown value")
        data.seqType.name = SeqTypeNames.CHIP_SEQ.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.createSeqTrack(
                            data.dataFile,
                            data.run,
                            data.sample,
                            data.seqType,
                            "1",
                            data.softwareTool
                            )
        }
    }

    @Test
    void testCreateSeqTrackChipSeqAntibodyTargetWithEmptyValue() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.ANTIBODY_TARGET, "")
        data.seqType.name = SeqTypeNames.CHIP_SEQ.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.createSeqTrack(
                            data.dataFile,
                            data.run,
                            data.sample,
                            data.seqType,
                            "1",
                            data.softwareTool
                            )
        }
    }

    @Test
    void testCreateSeqTrackChipSeqWithValidAntibodyTarget() {
        Map data = createData()
        DomainFactory.createMetaDataKeyAndEntry(data.dataFile, MetaDataColumn.ANTIBODY_TARGET, ANTIBODY_TARGET_IDENTIFIER)
        data.seqType.name = SeqTypeNames.CHIP_SEQ.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(ChipSeqSeqTrack.class, seqTrack.class)
    }



    private DataFile createAndSaveDataFileAndMetaDataEntry(Map<MetaDataKey, String> metaDataEntries, Run run = null, RunSegment runSegment = null) {
        DataFile dataFile = new DataFile(
                fileName: "1_ACTGTG_L005_R1_complete_filtered.fastq.gz",
                laneNr: LANE_NR,
                run: run,
                fileType: FileType.buildLazy(type: FileType.Type.SEQUENCE),
                runSegment: runSegment,
        )
        assertNotNull(dataFile.save())
        metaDataEntries.each { metaDataKey, value ->
            MetaDataKey key = MetaDataKey.buildLazy(name: metaDataKey)

            MetaDataEntry metaDataEntry = new MetaDataEntry(value: value, dataFile: dataFile, key: key, source: MetaDataEntry.Source.SYSTEM)
            assertNotNull(metaDataEntry.save())
        }

        return dataFile
    }

    private Map createData() {
        Map map = [:]
        Project project = TestData.createProject(
                        name: "name",
                        dirName: "dirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true]))

        SeqPlatform seqPlatform = SeqPlatform.build()

        SeqCenter seqCenter = new SeqCenter(
                        name: "seqCenter",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        Run run = new Run(
                        name: "run",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: Run.StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))

        Individual individual = new Individual(
                        pid: "pid",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: de.dkfz.tbi.otp.ngsdata.Individual.Type.REAL,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sampletype"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SeqType seqType = new SeqType(
                        name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                        libraryLayout: "library",
                        dirName: "dirName"
                        )
        assertNotNull(seqType.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "softwaretool",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        DataFile dataFile = new DataFile(
                        fileName: "1_barcode_L005_R2_complete_filtered.fastq.gz",
                        run: run
                        )
        assertNotNull(dataFile.save([flush: true]))

        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                        name: LIBRARY_PREPARATION_KIT_NAME_VALID,
                        shortDisplayName: LIBRARY_PREPARATION_KIT_NAME_VALID,
                        )
        assertNotNull(libraryPreparationKit.save([flush: true]))

        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                        name: LIBRARY_PREPARATION_KIT_SYNONYM_VALID,
                        libraryPreparationKit: libraryPreparationKit)
        assertNotNull(libraryPreparationKitSynonym.save([flush: true]))

        AntibodyTarget antibodyTarget = new AntibodyTarget(
                        name: ANTIBODY_TARGET_IDENTIFIER)
        assertNotNull(antibodyTarget.save([flush: true]))

        return [
            dataFile: dataFile,
            run: run,
            sample: sample,
            seqType: seqType,
            softwareTool: softwareTool,
            libraryPreparationKit: libraryPreparationKit,
            antibodyTarget: antibodyTarget,
        ]
    }



    def addDataFile = { SeqTrack seqTrack ->
        final DataFile dataFile = testData.createDataFile()
        dataFile.seqTrack = seqTrack
        assertNotNull(dataFile.save(flush: true))
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_DataFileIsNull() {
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.extractAndSetLibraryPreparationKit(null, new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline), run, sample)
        }
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_BuilderIsNull() {
        Run run = new Run()
        DataFile dataFile = new DataFile()
        String lane = "lane"
        Sample sample = new Sample()
        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.extractAndSetLibraryPreparationKit(dataFile, null, run, sample)
        }
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_RunIsNull() {
        DataFile dataFile = new DataFile()
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.extractAndSetLibraryPreparationKit(dataFile, new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline), null, sample)
        }
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_SampleIsNull() {
        DataFile dataFile = new DataFile()
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.extractAndSetLibraryPreparationKit(dataFile, new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline), run, null)
        }
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_MetaDataEntryIsNull() {
        testData.createObjects()
        DataFile dataFile = testData.createDataFile()
        assertNotNull(dataFile.save(flush: true))
        DomainFactory.createMetaDataKeyAndEntry(dataFile, MetaDataColumn.LIB_PREP_KIT, "testValue")
        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(LANE_NR, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        seqTrackService.extractAndSetLibraryPreparationKit(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrackBuilder.informationReliability)
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_InformationReliabilityIsUNKNOWN_VERIFIED() {
        testData.createObjects()
        DomainFactory.createMetaDataKeyAndEntry(testData.dataFile, MetaDataColumn.LIB_PREP_KIT, UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE)
        testData.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(testData.seqType.save([flush: true]))
        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(LANE_NR, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        seqTrackService.extractAndSetLibraryPreparationKit(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
        assertEquals(InformationReliability.UNKNOWN_VERIFIED, seqTrackBuilder.informationReliability)
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_KitIsNull() {
        testData.createObjects()
        DomainFactory.createMetaDataKeyAndEntry(testData.dataFile, MetaDataColumn.LIB_PREP_KIT, LIBRARY_PREPARATION_KIT_NAME_INVALID)
        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(LANE_NR, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        TestCase.shouldFail(IllegalArgumentException) {
            seqTrackService.extractAndSetLibraryPreparationKit(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
        }
    }


    @Test
    void testExtractAndSetLibraryPreparationKit_KitAvailableAndVerified() {
        testData.createObjects()
        DomainFactory.createMetaDataKeyAndEntry(testData.dataFile, MetaDataColumn.LIB_PREP_KIT, LIBRARY_PREPARATION_KIT_NAME_VALID)
        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(LANE_NR, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)
        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME_VALID)

        assertNull(seqTrackBuilder.libraryPreparationKit)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrackBuilder.informationReliability)
        seqTrackService.extractAndSetLibraryPreparationKit(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
        assertEquals(libraryPreparationKit, seqTrackBuilder.libraryPreparationKit)
        assertEquals(InformationReliability.KNOWN, seqTrackBuilder.informationReliability)
    }



    @Test
    void testAnnotateSeqTrackForChipSeqDataFileIsNull() {
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline)
        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.annotateSeqTrackForChipSeq(null, seqTrackBuilder)
        }
    }


    @Test
    void testAnnotateSeqTrackForChipSeqBuilderIsNull() {
        DataFile dataFile = new DataFile()
        TestCase.shouldFail(IllegalArgumentException) {
            seqTrackService.annotateSeqTrackForChipSeq(dataFile, null)
        }
    }


    @Test
    void testAnnotateSeqTrackForChipSeqMetaDataEntryANTIBODY_TARGETIsNull() {
        testData.createObjects()
        DataFile dataFile = new DataFile()
        assertNotNull(dataFile.save(flush: true))

        MetaDataKey metaDataKey = new MetaDataKey(
                        name: MetaDataColumn.ANTIBODY_TARGET.name()
                        )
        assertNotNull(metaDataKey.save(flush: true))

        MetaDataEntry metaDataEntry = new MetaDataEntry(
                        value: "Agilent SureSelect V3",
                        dataFile: dataFile,
                        key: metaDataKey,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry.save(flush: true))

        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder("lane", testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        TestCase.shouldFail(IllegalArgumentException.class) {
            seqTrackService.annotateSeqTrackForChipSeq(testData.dataFile, seqTrackBuilder)
        }
    }


    @Test
    void testAnnotateSeqTrackForChipSeqMetaDataEntryANTIBODYIsNull() {
        testData.createObjects()
        DataFile dataFile = new DataFile()
        assertNotNull(dataFile.save(flush: true))

        MetaDataKey metaDataKey1 = new MetaDataKey(
                        name: MetaDataColumn.ANTIBODY_TARGET.name()
                        )
        assertNotNull(metaDataKey1.save(flush: true))

        MetaDataKey metaDataKey2 = new MetaDataKey(
                        name: MetaDataColumn.ANTIBODY.name()
                        )
        assertNotNull(metaDataKey2.save(flush: true))

        MetaDataEntry metaDataEntry1 = new MetaDataEntry(
                        value: ANTIBODY_TARGET_IDENTIFIER,
                        dataFile: testData.dataFile,
                        key: metaDataKey1,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry1.save(flush: true))

        MetaDataEntry metaDataEntry2 = new MetaDataEntry(
                        value: ANTIBODY_IDENTIFIER,
                        dataFile: dataFile,
                        key: metaDataKey2,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry2.save(flush: true))

        AntibodyTarget antibodyTarget = new AntibodyTarget(
                        name: ANTIBODY_TARGET_IDENTIFIER
                        )
        assertNotNull(antibodyTarget.save(flush: true))

        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder("lane", testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        assertNull(seqTrackBuilder.antibody)
        assertNull(seqTrackBuilder.antibodyTarget)
        seqTrackService.annotateSeqTrackForChipSeq(testData.dataFile, seqTrackBuilder)
        assertNull(seqTrackBuilder.antibody)
        assertEquals(ANTIBODY_TARGET_IDENTIFIER, seqTrackBuilder.antibodyTarget.name)
    }


    @Test
    void testAnnotateSeqTrackForChipSeqMetaDataEntry() {
        testData.createObjects()

        MetaDataKey metaDataKey1 = new MetaDataKey(
                        name: MetaDataColumn.ANTIBODY_TARGET.name()
                        )
        assertNotNull(metaDataKey1.save(flush: true))

        MetaDataKey metaDataKey2 = new MetaDataKey(
                        name: MetaDataColumn.ANTIBODY.name()
                        )
        assertNotNull(metaDataKey2.save(flush: true))

        MetaDataEntry metaDataEntry1 = new MetaDataEntry(
                        value: ANTIBODY_TARGET_IDENTIFIER,
                        dataFile: testData.dataFile,
                        key: metaDataKey1,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry1.save(flush: true))

        MetaDataEntry metaDataEntry2 = new MetaDataEntry(
                        value: ANTIBODY_IDENTIFIER,
                        dataFile: testData.dataFile,
                        key: metaDataKey2,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry2.save(flush: true))

        AntibodyTarget antibodyTarget = new AntibodyTarget(
                        name: ANTIBODY_TARGET_IDENTIFIER
                        )
        assertNotNull(antibodyTarget.save(flush: true))

        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder("lane", testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        assertNull(seqTrackBuilder.antibody)
        assertNull(seqTrackBuilder.antibodyTarget)
        seqTrackService.annotateSeqTrackForChipSeq(testData.dataFile, seqTrackBuilder)
        assertEquals(ANTIBODY_IDENTIFIER, seqTrackBuilder.antibody)
        assertEquals(ANTIBODY_TARGET_IDENTIFIER, seqTrackBuilder.antibodyTarget.name)
    }

    @Test
    void testDecideAndPrepareForAlignment_defaultDecider_shouldReturnOneWorkPackage() {
        testData.createObjects()
        setupProjectAndDataFile("defaultOtpAlignmentDecider")

        Collection<MergingWorkPackage> workPackages = seqTrackService.decideAndPrepareForAlignment(testData.seqTrack)

        assert workPackages.size() == 1
        assert workPackages.iterator().next().seqType == testData.seqType
    }

    @Test
    void testDecideAndPrepareForAlignment_noAlignmentDecider_shouldReturnEmptyList() {
        testData.createObjects()
        setupProjectAndDataFile("noAlignmentDecider")

        Collection<MergingWorkPackage> workPackages = seqTrackService.decideAndPrepareForAlignment(testData.seqTrack)

        assert workPackages.empty
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_InputIsNull_ShouldFail() {
        shouldFail(IllegalArgumentException) {
            seqTrackService.returnExternallyProcessedMergedBamFiles(null)
        }
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_InputIsEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            seqTrackService.returnExternallyProcessedMergedBamFiles([])
        }
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_NoExternalBamFileAttached_AllFine() {
        SeqTrack seqTrack = SeqTrack.build()
        assert seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack]).isEmpty()
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_ExternalBamFileAttached_AllFine() {
        SeqTrack seqTrack = SeqTrack.build()
        ExternallyProcessedMergedBamFile bamFile = ExternallyProcessedMergedBamFile.build(
                fastqSet: FastqSet.build(seqTracks: [seqTrack]),
                type: AbstractBamFile.BamType.RMDUP,
        ).save(flush: true)
        assert [bamFile] == seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack])
    }

    @Test
    void testGetAlignmentDecider_WhenNoAlignmentDeciderBeanName_ShouldFail() {
        Project project = Project.build()

        TestCase.shouldFail (RuntimeException) {
            seqTrackService.getAlignmentDecider(project)
        }
    }

    @Test
    void testGetAlignmentDecider_WhenAllFine_ShouldReturnAlignmentDecider() {
        Project project = Project.build(alignmentDeciderBeanName: "noAlignmentDecider")

        assert "NoAlignmentDecider" == seqTrackService.getAlignmentDecider(project).class.simpleName
    }


    @Test
    void testSetRunReadyForFastqc_SeqTracksReady() {
        DataFile dataFile = createDataFor_setRunReadyForFastqc()

        seqTrackService.setRunReadyForFastqc(dataFile.run)
        assert SeqTrack.DataProcessingState.NOT_STARTED == dataFile.seqTrack.fastqcState
    }

    @Test
    void testSetRunReadyForFastqc_NoSeqTracksReady() {
        DataFile dataFile = createDataFor_setRunReadyForFastqc()
        dataFile.seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert dataFile.save(flush: true, failOnError: true)

        seqTrackService.setRunReadyForFastqc(dataFile.run)
        assert SeqTrack.DataProcessingState.FINISHED == dataFile.seqTrack.fastqcState
    }

    @Test
    void testSetRunReadyForFastqc_MultipleSeqTracksReady() {
        DataFile dataFile1 = createDataFor_setRunReadyForFastqc()
        DataFile dataFile2 = createDataFor_setRunReadyForFastqc()
        dataFile2.runSegment.run  = dataFile1.runSegment.run
        dataFile2.run  = dataFile1.run
        assert dataFile2.runSegment.save(flush: true, failOnError: true)
        assert dataFile2.save(flush: true, failOnError: true)

        seqTrackService.setRunReadyForFastqc(dataFile1.run)
        assert SeqTrack.DataProcessingState.NOT_STARTED == dataFile1.seqTrack.fastqcState
        assert SeqTrack.DataProcessingState.NOT_STARTED == dataFile2.seqTrack.fastqcState
    }


    private DataFile createDataFor_setRunReadyForFastqc() {
        Run run = DomainFactory.createRun()
        RunSegment runSegment = DomainFactory.createRunSegment(
                run: run,
                filesStatus: RunSegment.FilesStatus.FILES_CORRECT,
        )
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                run: run
        )
        DataFile dataFile = DomainFactory.buildSequenceDataFile(
                seqTrack: seqTrack,
                run: runSegment.run,
                runSegment: runSegment,
                project: seqTrack.project
        )
        return dataFile
    }




    private void setupProjectAndDataFile(String decider) {
        testData.project.alignmentDeciderBeanName = decider
        testData.project.save(failOnError: true)

        testData.dataFile.fileExists = true
        testData.dataFile.fileSize = 1L
        testData.dataFile.fileType = testData.fileType
        testData.dataFile.save(failOnError: true)
    }
}
