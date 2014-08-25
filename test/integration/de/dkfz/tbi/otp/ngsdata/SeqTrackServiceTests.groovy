package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.MetaDataEntry.Source
import de.dkfz.tbi.otp.ngsdata.MetaDataEntry.Status
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class SeqTrackServiceTests extends AbstractIntegrationTest {

    SeqTrackService seqTrackService

    TestData testData

    File dataPath
    File mdPath

    static final String ANTIBODY_TARGET_IDENTIFIER = "AntibodyTargetIdentifier123"
    static final String ANTIBODY_IDENTIFIER = "AntibodyIdentifier123"

    // the String "UNKNOWN" is used instead of the enum, because that is how it appears in external input files
    final String UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE = "UNKNOWN"

    @Before
    void setUp() {
        // TODO needs rewritting
        dataPath = new File("/tmp/otp/dataPath")
        mdPath = new File("/tmp/otp/mdPath")
        if(!dataPath.isDirectory()) {
            dataPath.mkdirs()
            assertTrue(dataPath.isDirectory())
        }
        if(!mdPath.isDirectory()) {
            mdPath.mkdirs()
            assertTrue(mdPath.isDirectory())
        }

        testData = new TestData()
    }

    @After
    void tearDown() {
        dataPath.deleteDir()
        mdPath.deleteDir()
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
        SeqPlatform seqPlatform = new SeqPlatform(name: "testSolid")
        assert(seqPlatform.save())
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
        Project project = new Project(name: "testProject", dirName: "testDir", host: "dkfz", realm: realm)
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

    void testAssertConsistentLibraryPreparationKitNoLibPrepKitKey() {
        DataFile dataFileR1 = new DataFile(fileName: "1_ACTGTG_L005_R1_complete_filtered.fastq.gz")
        assertNotNull(dataFileR1.save())
        DataFile dataFileR2 = new DataFile(fileName: "1_ACTGTG_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFileR2.save())

        List<DataFile> dataFiles = [dataFileR1, dataFileR2]
        seqTrackService.assertConsistentLibraryPreparationKit(dataFiles)
    }

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

    void testCreateSeqTrackNoExome() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "")
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

    void testCreateSeqTrackNoExomeInvalid() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "")
        shouldFail(IllegalArgumentException.class) {
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

    void testCreateSeqTrackNoExomeNo_LIB_PREP_KIT_MetaData() {
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

    void testCreateSeqTrackExomeByKit() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "ExomeEnrichmentKit")
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
        assertEquals(InformationReliability.KNOWN, seqTrack.kitInfoReliability)
        assertEquals(data.exomeEnrichmentKit, seqTrack.exomeEnrichmentKit)
    }

    void testCreateSeqTrackExomeByKitIdentifier() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "ExomeEnrichmentKitSynonym")
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
        assertEquals(InformationReliability.KNOWN, seqTrack.kitInfoReliability)
        assertEquals(data.exomeEnrichmentKit, seqTrack.exomeEnrichmentKit)
    }


    void testCreateSeqTrackExomeByValueUnknown() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE)
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
        assertNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateSeqTrackExomeInvalidByValueUnknownUnverified() {
        //value UNKNOWN_UNVERIFIED can not given in meta data, so this value should be handled like an unknown value
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "UNKNOWN_UNVERIFIED")
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        shouldFail(IllegalArgumentException.class) {
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

    void testCreateSeqTrackExomeInvalidUnknownValue() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "unknown value")
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        shouldFail(IllegalArgumentException.class) {
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

    void testCreateSeqTrackExomeInvalidEmptyValue() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "")
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        shouldFail(IllegalArgumentException.class) {
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

    void testCreateSeqTrackExomeInvalidNoSample() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.LIB_PREP_KIT, "ExomeEnrichmentKit")
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        shouldFail(IllegalArgumentException.class) {
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

    void testCreateSeqTrackExomeNoEnrichmentkitColumnKey() {
        Map data = createData()
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))
        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(ExomeSeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrack.kitInfoReliability)
        assertNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateSeqTrackExomeNoEnrichmentkitColumn() {
        Map data = createData()
        data.seqType.name = SeqTypeNames.EXOME.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))
        MetaDataKey metaDataKey = new MetaDataKey(
                        name: "LIB_PREP_KIT"
                        )
        assertNotNull(metaDataKey.save([flush: true]))

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(ExomeSeqTrack.class, seqTrack.class)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrack.kitInfoReliability)
        assertNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateSeqTrackChipSeqAntibodyTargetWithUnknownValue() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.ANTIBODY_TARGET, "unknown value")
        data.seqType.name = SeqTypeNames.CHIP_SEQ.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        shouldFail(IllegalArgumentException.class) {
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

    void testCreateSeqTrackChipSeqAntibodyTargetWithEmptyValue() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.ANTIBODY_TARGET, "")
        data.seqType.name = SeqTypeNames.CHIP_SEQ.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        shouldFail(IllegalArgumentException.class) {
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

    void testCreateSeqTrackChipSeqWithValidAntibodyTarget() {
        Map data = createData()
        createMetaData(data.dataFile, MetaDataColumn.ANTIBODY_TARGET, ANTIBODY_TARGET_IDENTIFIER)
        data.seqType.name = SeqTypeNames.CHIP_SEQ.seqTypeName
        assertNotNull(data.seqType.save([flush: true]))

        SeqTrack seqTrack = seqTrackService.createSeqTrack(
                        data.dataFile,
                        data.run,
                        data.sample,,
                        data.seqType,
                        "1",
                        data.softwareTool
                        )
        assertNotNull(seqTrack)
        assertEquals(ChipSeqSeqTrack.class, seqTrack.class)
    }

    private Map createData() {
        Map map = [:]
        Project project = new Project(
                        name: "name",
                        dirName: "dirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
                        name: "seqplatform",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

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
                        name: "seqtype",
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

        ExomeEnrichmentKit exomeEnrichmentKit = new ExomeEnrichmentKit(
                        name: "ExomeEnrichmentKit"
                        )
        assertNotNull(exomeEnrichmentKit.save([flush: true]))

        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = new ExomeEnrichmentKitSynonym(
                        name: "ExomeEnrichmentKitSynonym",
                        exomeEnrichmentKit: exomeEnrichmentKit)
        assertNotNull(exomeEnrichmentKitSynonym.save([flush: true]))

        AntibodyTarget antibodyTarget = new AntibodyTarget(
                        name: ANTIBODY_TARGET_IDENTIFIER)
        assertNotNull(antibodyTarget.save([flush: true]))

        return [
            dataFile: dataFile,
            run: run,
            sample: sample,
            seqType: seqType,
            softwareTool: softwareTool,
            exomeEnrichmentKit: exomeEnrichmentKit,
            antibodyTarget: antibodyTarget
        ]
    }

    private void createMetaData(DataFile dataFile, MetaDataColumn column, String value) {
        MetaDataKey metaDataKey = new MetaDataKey(
                        name: column as String
                        )
        assertNotNull(metaDataKey.save([flush: true]))

        MetaDataEntry metaDataEntry = new MetaDataEntry(
                        value: value,
                        dataFile: dataFile,
                        key: metaDataKey,
                        source: MetaDataEntry.Source.SYSTEM
                        )
        assertNotNull(metaDataEntry.save())
    }


    def addDataFile = { SeqTrack seqTrack ->
        final DataFile dataFile = testData.createDataFile()
        dataFile.seqTrack = seqTrack
        assertNotNull(dataFile.save(flush: true))
    }


    public void testSetRunReadyForAlignment() {
        /* The criteria in AlignmentPassService.ALIGNABLE_SEQTRACK_HQL are deeply tested by
         * AlignmentPassServiceIntegrationTests, so in this method we do not test it again.
         */

        testData.createObjects()

        final Run run1 = testData.run
        final Run run2 = new Run()
        run2.name = "testRun2"
        run2.seqCenter = testData.seqCenter
        run2.seqPlatform = testData.seqPlatform
        assertNotNull(run2.save(flush: true))

        final Sample sampleA = testData.sample
        final Sample sampleB = new Sample()
        sampleB.individual = testData.createIndividual()
        sampleB.individual.pid = "drölf"
        assertNotNull(sampleB.individual.save(flush: true))
        sampleB.sampleType = testData.sampleType
        assertNotNull(sampleB.save(flush: true))

        final SeqType alignableSeqType = new SeqType()
        alignableSeqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        alignableSeqType.libraryLayout = "PAIRED"
        alignableSeqType.dirName = "alignable_wgs"
        assertNotNull(alignableSeqType.save(flush: true))

        final SeqType alignableExomeSeqType = new SeqType()
        alignableExomeSeqType.name = SeqTypeNames.EXOME.seqTypeName
        alignableExomeSeqType.libraryLayout = "PAIRED"
        alignableExomeSeqType.dirName = "alignable_exome"
        assertNotNull(alignableExomeSeqType.save(flush: true))

        final SeqType nonAlignableSeqType = new SeqType()
        nonAlignableSeqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        nonAlignableSeqType.libraryLayout = "SCREWED"
        nonAlignableSeqType.dirName = "non_alignable_wgs"
        assertNotNull(nonAlignableSeqType.save(flush: true))

        final SeqType nonAlignableExomeSeqType = new SeqType()
        nonAlignableExomeSeqType.name = SeqTypeNames.EXOME.seqTypeName
        nonAlignableExomeSeqType.libraryLayout = "SCREWED"
        nonAlignableExomeSeqType.dirName = "non_alignable_exome"
        assertNotNull(nonAlignableExomeSeqType.save(flush: true))

        testData.dataFile.delete(flush: true)
        testData.seqTrack.delete(flush: true)

        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // no SeqTracks at all
        seqTrackService.setRunReadyForAlignment(run1)
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        final SeqTrack seqTrack1AY = testData.createSeqTrack()
        seqTrack1AY.run = run1
        seqTrack1AY.sample = sampleA
        seqTrack1AY.seqType = alignableSeqType
        seqTrack1AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack1AY.save(flush: true))
        addDataFile(seqTrack1AY)
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // no SeqTrack in this run
        seqTrackService.setRunReadyForAlignment(run2)
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        final SeqTrack seqTrack2AN = testData.createSeqTrack()
        seqTrack2AN.run = run2
        seqTrack2AN.sample = sampleA
        seqTrack2AN.seqType = nonAlignableSeqType
        seqTrack2AN.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack2AN.save(flush: true))
        addDataFile(seqTrack2AN)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // no alignable SeqTrack in this run
        seqTrackService.setRunReadyForAlignment(run2)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        final SeqTrack seqTrack2BY = testData.createSeqTrack()
        seqTrack2BY.run = run2
        seqTrack2BY.sample = sampleB
        seqTrack2BY.seqType = alignableSeqType
        seqTrack2BY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack2BY.save(flush: true))
        addDataFile(seqTrack2BY)
        assertEquals(3, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // alignable SeqTrack, no other alignable SeqTrack for the sample
        seqTrackService.setRunReadyForAlignment(run2)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2BY.alignmentState)

        // alignable SeqTrack (already NOT_STARTED), no other alignable SeqTrack for the sample
        seqTrackService.setRunReadyForAlignment(run2)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        final SeqTrack seqTrack2AY = testData.createSeqTrack()
        seqTrack2AY.run = run2
        seqTrack2AY.sample = sampleA
        seqTrack2AY.seqType = alignableSeqType
        seqTrack2AY.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        assertNotNull(seqTrack2AY.save(flush: true))
        addDataFile(seqTrack2AY)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // alignable SeqTrack (already NOT_STARTED), other alignable SeqTrack for the sample in another run
        seqTrackService.setRunReadyForAlignment(run2)
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(3, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack1AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2BY.alignmentState)


        final ExomeSeqTrack exomeSeqTrack3AY = testData.createExomeSeqTrack(run1)
        exomeSeqTrack3AY.run = run1
        exomeSeqTrack3AY.sample = sampleA
        exomeSeqTrack3AY.seqType = alignableExomeSeqType
        exomeSeqTrack3AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(exomeSeqTrack3AY.save(flush: true))
        addDataFile(exomeSeqTrack3AY)

        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(3, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        seqTrackService.setRunReadyForAlignment(run2)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(3, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack1AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2BY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.UNKNOWN, exomeSeqTrack3AY.alignmentState)

        def reset = {
            seqTrack1AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
            assertNotNull(seqTrack1AY.save(flush: true))
            seqTrack2AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
            assertNotNull(seqTrack2AY.save(flush: true))
            seqTrack2BY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
            assertNotNull(seqTrack2BY.save(flush: true))
            seqTrack2AN.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
            assertNotNull(seqTrack2AN.save(flush: true))
            exomeSeqTrack3AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
            assertNotNull(exomeSeqTrack3AY.save(flush: true))
            println SeqTrack.findByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN)
            assertEquals(5, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
            assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        }

        reset()

        // alignable SeqTrack, other alignable SeqTrack for the sample in another run
        seqTrackService.setRunReadyForAlignment(run1)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(3, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack1AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2AY.alignmentState)

        reset()

        // two alignable SeqTracks, other alignable SeqTrack for the sample in another run
        seqTrackService.setRunReadyForAlignment(run2)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(3, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack1AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack2BY.alignmentState)

        shouldFail(IllegalArgumentException.class) {
            seqTrackService.setRunReadyForAlignment(null)
        }
    }


    public void testSetRunReadyForExomeAlignment() {
        /* The criteria in AlignmentPassService.ALIGNABLE_SEQTRACK_HQL are deeply tested by
         * AlignmentPassServiceIntegrationTests, so in this method we do not test it again.
         */

        testData.createObjects()

        final Run run1 = testData.run

        final Sample sampleA = testData.sample

        final SeqType alignableExomeSeqType = new SeqType()
        alignableExomeSeqType.name = SeqTypeNames.EXOME.seqTypeName
        alignableExomeSeqType.libraryLayout = "PAIRED"
        alignableExomeSeqType.dirName = "alignable_seq_type_dir_name"
        assertNotNull(alignableExomeSeqType.save(flush: true))

        final SeqType nonAlignableExomeSeqType = new SeqType()
        nonAlignableExomeSeqType.name = SeqTypeNames.EXOME.seqTypeName
        nonAlignableExomeSeqType.libraryLayout = "SCREWED"
        nonAlignableExomeSeqType.dirName = "non_alignable_seq_type_dir_name"
        assertNotNull(nonAlignableExomeSeqType.save(flush: true))

        testData.dataFile.delete(flush: true)
        testData.seqTrack.delete(flush: true)

        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // from here on the Exome-seqType is tested
        final ExomeSeqTrack seqTrack3AY = testData.createExomeSeqTrack(run1)
        seqTrack3AY.sample = sampleA
        seqTrack3AY.seqType = alignableExomeSeqType
        seqTrack3AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack3AY.save(flush: true))
        addDataFile(seqTrack3AY)

        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        final ExomeSeqTrack seqTrack3AN = testData.createExomeSeqTrack(run1)
        seqTrack3AN.sample = sampleA
        seqTrack3AN.seqType = nonAlignableExomeSeqType
        seqTrack3AN.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack3AN.save(flush: true))
        addDataFile(seqTrack3AN)

        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // When it is verified that no kit is available for a seqTrack, it will not be aligned -> remains DataProcessingState.UNKNOWN
        seqTrack3AY.kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
        assertNotNull(seqTrack3AY.save(flush: true))
        seqTrack3AN.kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
        assertNotNull(seqTrack3AN.save(flush: true))

        seqTrackService.setRunReadyForAlignment(run1)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        // When it is not verified that there is no kit available for the seqTrack the state changes to DataProcessingState.NOT_STARTED,
        // so that it can be detected by the Alignment-WF to be "INVALID" (AlignmentPassService.findAlignableSeqTrack()).
        //Of course it will only be changed if the seqTrack shall be aligned
        seqTrack3AY.kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
        assertNotNull(seqTrack3AY.save(flush: true))
        seqTrack3AN.kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
        assertNotNull(seqTrack3AN.save(flush: true))

        seqTrackService.setRunReadyForAlignment(run1)
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack3AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.UNKNOWN, seqTrack3AN.alignmentState)

        // In the case that there is an EnrichmentKit available for the ExomeSeqTrack it will be aligned.
        // The kitInfoReliabilityState is not important anymore
        seqTrack3AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack3AY.save(flush: true))
        seqTrack3AN.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack3AN.save(flush: true))

        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit("Agilent SureSelect V3")
        testData.addKitToExomeSeqTrack(seqTrack3AY, exomeEnrichmentKit)
        testData.addKitToExomeSeqTrack(seqTrack3AN, exomeEnrichmentKit)

        seqTrackService.setRunReadyForAlignment(run1)
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(1, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack3AY.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.UNKNOWN, seqTrack3AN.alignmentState)

        // Only seqTracks, belonging to a RunSegment which shall be be aligned (align = true), will be aligned
        seqTrack3AY.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack3AY.save(flush: true))
        seqTrack3AN.alignmentState = SeqTrack.DataProcessingState.UNKNOWN
        assertNotNull(seqTrack3AN.save(flush: true))

        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))


        RunSegment runSegment = testData.runSegment
        runSegment.align = false
        assertNotNull(runSegment.save(flush: true))

        seqTrackService.setRunReadyForAlignment(run1)
        assertEquals(2, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.UNKNOWN))
        assertEquals(0, SeqTrack.countByAlignmentState(SeqTrack.DataProcessingState.NOT_STARTED))

        shouldFail(IllegalArgumentException.class) {
            seqTrackService.setRunReadyForAlignment(null)
        }
    }


    @Test(expected = IllegalArgumentException)
    void testAnnotateSeqTrackForExomeDataFileIsNull() {
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        seqTrackService.annotateSeqTrackForExome(null, new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline), run, sample)
    }


    @Test(expected = IllegalArgumentException)
    void testAnnotateSeqTrackForExomeBuilderIsNull() {
        Run run = new Run()
        DataFile dataFile = new DataFile()
        String lane = "lane"
        Sample sample = new Sample()
        seqTrackService.annotateSeqTrackForExome(dataFile, null, run, sample)
    }


    @Test(expected = IllegalArgumentException)
    void testAnnotateSeqTrackForExomeRunIsNull() {
        DataFile dataFile = new DataFile()
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        seqTrackService.annotateSeqTrackForExome(dataFile, new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline), null, sample)
    }


    @Test(expected = IllegalArgumentException)
    void testAnnotateSeqTrackForExomeSampleIsNull() {
        DataFile dataFile = new DataFile()
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        seqTrackService.annotateSeqTrackForExome(dataFile, new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline), run, null)
    }


    @Test
    void testAnnotateSeqTrackForExomeMetaDataEntryIsNull() {
        testData.createObjects()
        String lane = "lane"
        DataFile dataFile = testData.createDataFile()
        assertNotNull(dataFile.save(flush: true))

        MetaDataKey metaDataKey = new MetaDataKey(
                        name: MetaDataColumn.LIB_PREP_KIT.name()
                        )
        assertNotNull(metaDataKey.save(flush: true))

        MetaDataEntry metaDataEntry = new MetaDataEntry(
                        value: "testValue",
                        dataFile: dataFile,
                        key: metaDataKey,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry.save(flush: true))

        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(lane, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        seqTrackService.annotateSeqTrackForExome(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrackBuilder.informationReliability)
    }


    @Test
    void testAnnotateSeqTrackForExomeInformationReliabilityIsUNKNOWN_VERIFIED() {
        testData.createObjects()
        String lane = "lane"
        MetaDataKey metaDataKey = new MetaDataKey(
                        name: MetaDataColumn.LIB_PREP_KIT.name()
                        )
        assertNotNull(metaDataKey.save(flush: true))

        MetaDataEntry metaDataEntry = new MetaDataEntry(
                        value: UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE,
                        dataFile: testData.dataFile,
                        key: metaDataKey,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry.save(flush: true))

        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(lane, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        seqTrackService.annotateSeqTrackForExome(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
        assertEquals(InformationReliability.UNKNOWN_VERIFIED, seqTrackBuilder.informationReliability)
    }


    @Test(expected = IllegalArgumentException)
    void testAnnotateSeqTrackForExomeKitIsNull() {
        testData.createObjects()
        String lane = "lane"
        MetaDataKey metaDataKey = new MetaDataKey(
                        name: MetaDataColumn.LIB_PREP_KIT.name()
                        )
        assertNotNull(metaDataKey.save(flush: true))

        MetaDataEntry metaDataEntry = new MetaDataEntry(
                        value: "notExistingKit",
                        dataFile: testData.dataFile,
                        key: metaDataKey,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry.save(flush: true))

        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(lane, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        seqTrackService.annotateSeqTrackForExome(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
    }


    @Test
    void testAnnotateSeqTrackForExomeKitAvailableAndVerified() {
        testData.createObjects()
        String lane = "lane"
        MetaDataKey metaDataKey = new MetaDataKey(
                        name: MetaDataColumn.LIB_PREP_KIT.name()
                        )
        assertNotNull(metaDataKey.save(flush: true))

        MetaDataEntry metaDataEntry = new MetaDataEntry(
                        value: "Agilent SureSelect V3",
                        dataFile: testData.dataFile,
                        key: metaDataKey,
                        status: Status.VALID,
                        source: Source.MDFILE,
                        )
        assertNotNull(metaDataEntry.save(flush: true))

        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit("Agilent SureSelect V3")

        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(lane, testData.run, testData.sample, testData.seqType, testData.seqPlatform, testData.softwareTool)

        assertNull(seqTrackBuilder.exomeEnrichmentKit)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrackBuilder.informationReliability)
        seqTrackService.annotateSeqTrackForExome(testData.dataFile, seqTrackBuilder, testData.run, testData.sample)
        assertEquals(exomeEnrichmentKit, seqTrackBuilder.exomeEnrichmentKit)
        assertEquals(InformationReliability.KNOWN, seqTrackBuilder.informationReliability)
    }


    @Test(expected = IllegalArgumentException)
    void testAnnotateSeqTrackForChipSeqDataFileIsNull() {
        Run run = new Run()
        String lane = "lane"
        Sample sample = new Sample()
        SeqType seqType = new SeqType()
        SoftwareTool pipeline = new SoftwareTool()
        SeqPlatform seqPlatform = new SeqPlatform()
        SeqTrackBuilder seqTrackBuilder = new SeqTrackBuilder(lane, run, sample, seqType, seqPlatform, pipeline)
        seqTrackService.annotateSeqTrackForChipSeq(null, seqTrackBuilder)
    }


    @Test(expected = IllegalArgumentException)
    void testAnnotateSeqTrackForChipSeqBuilderIsNull() {
        DataFile dataFile = new DataFile()
        seqTrackService.annotateSeqTrackForChipSeq(dataFile, null)
    }


    @Test(expected = IllegalArgumentException)
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

        seqTrackService.annotateSeqTrackForChipSeq(testData.dataFile, seqTrackBuilder)
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
}
