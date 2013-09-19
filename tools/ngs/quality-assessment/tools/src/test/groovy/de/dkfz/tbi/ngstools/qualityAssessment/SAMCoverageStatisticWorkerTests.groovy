package de.dkfz.tbi.ngstools.qualityAssessment

import groovy.util.GroovyTestCase;
import net.sf.samtools.SAMRecord
import net.sf.samtools.SAMSequenceRecord
import org.junit.*

class SAMCoverageStatisticWorkerTests extends GroovyTestCase {

    private ChromosomeStatisticWrapper chromosome

    private SAMCoverageStatisticWorker samCoverageStatisticWorker

    private SAMRecord record

    private SAMSequenceRecord recordHeader

    private Parameters parameters

    @Before
    public void setUp() throws Exception {
        chromosome = new ChromosomeStatisticWrapper("TEST", 10)
        //countingStatisticWorker.parameters = new Parameters()
        record = new SAMRecord()
        recordHeader = new SAMSequenceRecord("chr10", 10)
        parameters = new Parameters(winSize: 10, mappingQuality: 5, coverageMappingQualityThreshold: 5)
        samCoverageStatisticWorker = new SAMCoverageStatisticWorker()
        samCoverageStatisticWorker.setParameters(parameters)
    }

    @After
    public void tearDown() throws Exception {
        chromosome = null
        record = null
        recordHeader = null
        samCoverageStatisticWorker = null
        parameters = null
    }

    @Test
    public void testPreProcessEven() {

        chromosome = new ChromosomeStatisticWrapper("TEST", 20)
        samCoverageStatisticWorker.preProcess(chromosome)
        assertEquals(2, chromosome.coverageTable.size())
    }

    @Test
    public void testPreProcessNoSequenceLength() {
        chromosome = new ChromosomeStatisticWrapper("TEST", 0)
        samCoverageStatisticWorker.preProcess(chromosome)
        assertEquals(0, chromosome.coverageTable.size())
    }

    @Test
    public void testPreProcessOdd() {
        chromosome = new ChromosomeStatisticWrapper("TEST", 11)
        samCoverageStatisticWorker.preProcess(chromosome)
        assertEquals(2, chromosome.coverageTable.size())
    }

    @Test
    public void testCountCoverageEmptyTable1() {
        samCoverageStatisticWorker.preProcess(chromosome)
        record.setDuplicateReadFlag(true)
        samCoverageStatisticWorker.countCoverage(chromosome, record)
        List coverageList = chromosome.coverageTable
        List expCoverageList = [0]
        assertEquals(expCoverageList, coverageList)
    }

    @Test
    public void testCountCoverageEmptyTable2() {
        samCoverageStatisticWorker.preProcess(chromosome)
        record.setDuplicateReadFlag(false)
        record.setMappingQuality(0)
        samCoverageStatisticWorker.countCoverage(chromosome, record)
        List coverageList = chromosome.coverageTable
        List expCoverageList = [0]
        assertEquals(expCoverageList, coverageList)
    }

    @Test
    public void testCountCoverageIncreaseEven() {
        chromosome = new ChromosomeStatisticWrapper("TEST", 0)
        chromosome.coverageTable = new int[11]
        recordHeader.setSequenceLength(0)
        record.setDuplicateReadFlag(false)
        record.setMappingQuality(10)
        record.setAlignmentStart(100)
        samCoverageStatisticWorker.countCoverage(chromosome, record)
        List coverageList = chromosome.coverageTable
        List expCoverageList = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]
        assertEquals(expCoverageList, coverageList)
    }

    @Test
    public void testCountCoverageIncreaseOdd() {
        chromosome.coverageTable = new int[11]
        record.setDuplicateReadFlag(false)
        record.setMappingQuality(10)
        record.setAlignmentStart(49)
        samCoverageStatisticWorker.countCoverage(chromosome, record)
        assertEquals(10, record.getMappingQuality())
        assertEquals(49, record.getAlignmentStart())
        List coverageList = chromosome.coverageTable
        List expCoverageList = [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0]
        assertEquals(expCoverageList, coverageList)
        assertEquals(expCoverageList, coverageList)
    }
}
