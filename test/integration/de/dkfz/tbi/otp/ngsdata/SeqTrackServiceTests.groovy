package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.otp.job.processing.ProcessingException

class SeqTrackServiceTests extends AbstractIntegrationTest {

    SeqTrackService seqTrackService

    File dataPath
    File mdPath

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
    }

    @After
    void tearDown() {
        dataPath.deleteDir()
        mdPath.deleteDir()
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

    void testCreateSeqTrackNoExome() {
        Map data = createData()
        createMetaData(data.dataFile, "")
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
        createMetaData(data.dataFile, "")
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
        shouldFail(ProcessingException.class) {
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

    void testCreateSeqTrackExomeByKit() {
        Map data = createData()
        createMetaData(data.dataFile, "ExomeEnrichmentKit")
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
    }

    void testCreateSeqTrackExomeByKitIdentifier() {
        Map data = createData()
        createMetaData(data.dataFile, "ExomeEnrichmentKitIdentifier")
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
    }

    void testCreateSeqTrackExomeUnknownValue() {
        Map data = createData()
        createMetaData(data.dataFile, "unknown")
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

    void testCreateSeqTrackExomeInvalid() {
        Map data = createData()
        createMetaData(data.dataFile)
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

        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = new ExomeEnrichmentKitIdentifier(
                        name: "ExomeEnrichmentKitIdentifier",
                        exomeEnrichmentKit: exomeEnrichmentKit)
        assertNotNull(exomeEnrichmentKitIdentifier.save([flush: true]))

        return [
            dataFile: dataFile,
            run: run,
            sample: sample,
            seqType: seqType,
            softwareTool: softwareTool
        ]
    }

    private void createMetaData(DataFile dataFile, String value = "ExomeEnrichmentKitIdentifier") {
        MetaDataKey metaDataKey = new MetaDataKey(
                        name: "LIB_PREP_KIT"
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

}
