package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

class PicardMarkDuplicatesMetricsServiceTests {

    PicardMarkDuplicatesMetricsService picardMarkDuplicatesMetricsService

    Realm realm
    TestConfigService configService

    File metrics

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File testDirectory

    String directory
    String baseFile
    String basePath

    @Before
    void setUp() {
        testDirectory = tmpDir.newFolder('otp-test')
        directory = testDirectory.absolutePath + "/processing/project-dir/results_per_pid/patient/merging//sample-type/seq-type/library/DEFAULT/0/pass0"
        baseFile = "sample-type_patient_seq-type_library_merged.mdup_metrics.txt"
        basePath = "${directory}/${baseFile}"

        realm = DomainFactory.createRealm().save([flush: true])

        configService = new TestConfigService([
                        'otp.root.path': testDirectory.absolutePath + '/root',
                        'otp.processing.root.path': testDirectory.absolutePath + '/processing',
        ])

        File baseDir = new File(directory)
        metrics = new File(basePath)
        assertTrue(baseDir.exists() || baseDir.mkdirs())
        assertTrue(baseDir.setReadable(true))
        if (metrics.exists()) {
            assertTrue(metrics.delete())
        }
    }

    @After
    void tearDown() {
        realm = null
        if (metrics.exists()) {
            assertTrue(metrics.delete())
        }
        configService.clean()
    }

    @Test
    void testParseAndLoadMetricsForProcessedMergedBamFile() {
        metrics << """## net.sf.picard.metrics.StringHeader
# net.sf.picard.sam.MarkDuplicates INPUT=[...]
## net.sf.picard.metrics.StringHeader
# Started on: Thu Jul 25 14:54:32 CEST 2013

## METRICS CLASS\tnet.sf.picard.sam.DuplicationMetrics
LIBRARY\tUNPAIRED_READS_EXAMINED\tREAD_PAIRS_EXAMINED\tUNMAPPED_READS\tUNPAIRED_READ_DUPLICATES\tREAD_PAIR_DUPLICATES\tREAD_PAIR_OPTICAL_DUPLICATES\tPERCENT_DUPLICATION\tESTIMATED_LIBRARY_SIZE
6\t12588\t1422931\t128131\t2507\t6199\t5889\t0.005214\t3238250263
"""
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNotNull(processedMergedBamFile)
        assertTrue(picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile))
        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = PicardMarkDuplicatesMetrics.findByAbstractBamFile(processedMergedBamFile)
        assertNotNull(picardMarkDuplicatesMetrics)
        assertEquals("net.sf.picard.sam.DuplicationMetrics", picardMarkDuplicatesMetrics.metricsClass)
        assertEquals("6", picardMarkDuplicatesMetrics.library)
        assertEquals(12588l, picardMarkDuplicatesMetrics.unpaired_reads_examined)
        assertEquals(1422931l, picardMarkDuplicatesMetrics.read_pairs_examined)
        assertEquals(128131l, picardMarkDuplicatesMetrics.unmapped_reads)
        assertEquals(2507l, picardMarkDuplicatesMetrics.unpaired_read_duplicates)
        assertEquals(6199l, picardMarkDuplicatesMetrics.read_pair_duplicates)
        assertEquals(5889l, picardMarkDuplicatesMetrics.read_pair_optical_duplicates)
        assertEquals(0.005214, picardMarkDuplicatesMetrics.percent_duplication, 0)
        assertEquals(3238250263l, picardMarkDuplicatesMetrics.estimated_library_size)
    }

    @Test(expected = RuntimeException)
    void testParseAndLoadMetricsForProcessedMergedBamFileNoFile() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNotNull(processedMergedBamFile)
        picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testParseAndLoadMetricsForProcessedMergedBamFileEmptyFile() {
        metrics << ""
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNotNull(processedMergedBamFile)
        picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testParseAndLoadMetricsForProcessedMergedBamFileNoMetricsSection() {
        metrics << """## net.sf.picard.metrics.StringHeader
# net.sf.picard.sam.MarkDuplicates INPUT=[...]
## net.sf.picard.metrics.StringHeader
# Started on: Thu Jul 25 14:54:32 CEST 2013
"""
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNotNull(processedMergedBamFile)
        picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testParseAndLoadMetricsForProcessedMergedBamFileWrongMetricsHeader() {
        metrics << """## net.sf.picard.metrics.StringHeader
# net.sf.picard.sam.MarkDuplicates INPUT=[...]
## net.sf.picard.metrics.StringHeader
# Started on: Thu Jul 25 14:54:32 CEST 2013

## METRICS CLASS\tnet.sf.picard.sam.DuplicationMetrics
LIBRARY\tUNPAIRED_READS_EXAMINED\tREAD_PAIRS_EXAMINED\tUNMAPPED_READS\tUNPAIRED_READ_DUPLICATES\tREAD_PAIR_DUPLICATES\tREAD_PAIR_OPTICAL_DUPLICATES\tPERCENT_DUPLICATION
6\t12588\t1422931\t128131\t2507\t6199\t5889\t0.005214\t3238250263
"""
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNotNull(processedMergedBamFile)
        picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testParseAndLoadMetricsForProcessedMergedBamFileWrongValueCountTooLess() {
        metrics << """## net.sf.picard.metrics.StringHeader
# net.sf.picard.sam.MarkDuplicates INPUT=[...]
## net.sf.picard.metrics.StringHeader
# Started on: Thu Jul 25 14:54:32 CEST 2013

## METRICS CLASS\tnet.sf.picard.sam.DuplicationMetrics
LIBRARY\tUNPAIRED_READS_EXAMINED\tREAD_PAIRS_EXAMINED\tUNMAPPED_READS\tUNPAIRED_READ_DUPLICATES\tREAD_PAIR_DUPLICATES\tREAD_PAIR_OPTICAL_DUPLICATES\tPERCENT_DUPLICATION\tESTIMATED_LIBRARY_SIZE
6\t12588\t1422931\t128131\t2507\t6199\t5889\t0.005214
"""
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNotNull(processedMergedBamFile)
        picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testParseAndLoadMetricsForProcessedMergedBamFileWrongValueCountTooMany() {
        metrics << """## net.sf.picard.metrics.StringHeader
# net.sf.picard.sam.MarkDuplicates INPUT=[...]
## net.sf.picard.metrics.StringHeader
# Started on: Thu Jul 25 14:54:32 CEST 2013

## METRICS CLASS\tnet.sf.picard.sam.DuplicationMetrics
LIBRARY\tUNPAIRED_READS_EXAMINED\tREAD_PAIRS_EXAMINED\tUNMAPPED_READS\tUNPAIRED_READ_DUPLICATES\tREAD_PAIR_DUPLICATES\tREAD_PAIR_OPTICAL_DUPLICATES\tPERCENT_DUPLICATION\tESTIMATED_LIBRARY_SIZE
6\t12588\t1422931\t128131\t2507\t6199\t5889\t0.005214\t3238250263\t6
"""
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNotNull(processedMergedBamFile)
        picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile)
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile() {
        Project project = DomainFactory.createProject(
                        name: "project",
                        dirName: "project-dir",
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

        SeqType seqType = DomainFactory.createSeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
        )

        MergingWorkPackage mergingWorkPackage = new TestData().createMergingWorkPackage(
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

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        fileExists: true,
                        type: AbstractBamFile.BamType.MDUP,
                        ])
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))
        return processedMergedBamFile
    }
}
