package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.util.Environment
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

class ProcessedMergedBamFileServiceTests {

    ProcessedMergingFileService processedMergingFileService

    ProcessedMergedBamFileService processedMergedBamFileService

    Realm realm

    final static String directory = "/tmp/otp-unit-test/pmbfs/processing/project-dir/results_per_pid/patient/merging//sample-type/seq-type/library/DEFAULT/0/pass0"
    final static String baseFile = "sample-type_patient_seq-type_library_merged.mdup"
    final static String basePath = directory + "/" + baseFile

    File baseDir

    @Before
    void setUp() {
        realm = new Realm()
        realm.cluster = Realm.Cluster.DKFZ
        realm.rootPath = "/tmp/otp-unit-test/pmfs/root"
        realm.processingRootPath = "/tmp/otp-unit-test/pmbfs/processing"
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000
        realm.pbsOptions = ""
        realm.name = "realmName"
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.env = Environment.getCurrent().getName()
        realm.save([flush: true, failOnError: true])

        baseDir = new File(directory)
        File bam = new File(basePath + ".bam")
        File bai = new File(basePath + ".bam.bai")
        File metrics = new File(basePath + "_metrics.txt")
        assertTrue(baseDir.exists() || baseDir.mkdirs())
        assertTrue(baseDir.setReadable(true))
        [bam, bai, metrics].each { File file ->
            if (file.exists()) {
                assertTrue(file.delete())
            }
            file << "test"
            file.deleteOnExit()
        }
    }

    @After
    void tearDown() {
        realm = null
        assertTrue(baseDir.setReadable(true))
        baseDir.listFiles().each { File file ->
            assertTrue(file.setReadable(true))
            assertTrue(file.delete())
        }
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateBamMetricsFileFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamMetricsFileFileIsEmpty() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForMetrics(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamMetricsFileFileNotReadable() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForMetrics(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test
    void testUpdateBamMetricsFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertTrue(processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile))
    }


    @Test(expected = IllegalArgumentException)
    void testUpdateBamFileIndexFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileIndexFileIsEmpty() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForBai(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileIndexFileNotReadable() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForBai(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test
    void testUpdateBamFileIndex() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertTrue(processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateBamFileBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileFileNotReadable() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePath(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileFileIsEmpty() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePath(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test
    void testUpdateBamFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertTrue(processedMergedBamFileService.updateBamFile(processedMergedBamFile))
    }

    @Test
    void testCreateMergedBamFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = processedMergedBamFileService.createMergedBamFile(mergingPass)
        assertNotNull(processedMergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateMergedBamFile_mergingPassIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = processedMergedBamFileService.createMergedBamFile(null)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathForBaiBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.filePathForBai(processedMergedBamFile)
    }

    @Test
    void testFilePathForBai() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String pathExp = basePath + ".bam.bai"
        String pathAct = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFileNameForBaiBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.fileNameForBai(processedMergedBamFile)
    }

    @Test
    void testFileNameForBai() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String nameExp = baseFile + ".bam.bai"
        String nameAct = processedMergedBamFileService.fileNameForBai(processedMergedBamFile)
        assertEquals(nameExp, nameAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathForMetricsBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
    }

    @Test
    void testFilePathForMetrics() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String pathExp = basePath + "_metrics.txt"
        String pathAct = processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFileNameForMetricsBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        String nameAct = processedMergedBamFileService.fileNameForMetrics(processedMergedBamFile)
    }

    @Test
    void testFileNameForMetrics() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String nameExp = baseFile + "_metrics.txt"
        String nameAct = processedMergedBamFileService.fileNameForMetrics(processedMergedBamFile)
        assertEquals(nameExp, nameAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.filePath(processedMergedBamFile)
    }

    @Test
    void testFilePath() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String pathExp = basePath +".bam"
        assertEquals(pathExp, processedMergedBamFileService.filePath(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testFileNameBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.fileName(processedMergedBamFile)
    }

    @Test
    void testFileName() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertEquals(baseFile + ".bam", processedMergedBamFileService.fileName(processedMergedBamFile))
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass) {
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP
                        )
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))
        return processedMergedBamFile
    }

    private MergingPass createMergingPass() {
        Project project = new Project(
                        name: "project",
                        dirName: "project-dir",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))
        return mergingPass
    }
}
