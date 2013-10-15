package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.*

class SAMCountingStatisticWorkerExomeUnitTests extends GroovyTestCase {

    ChromosomeStatisticWrapper allChrWrapper

    ChromosomeStatisticWrapper chrWrapper

    SAMCountingStatisticWorker worker

    File bedFile = new File("/tmp/test-bed-file.perfect")

    File refGenMetaFile = new File("/tmp/test-ref-gen.perfect")


    void setUp() {
        refGenMetaFile << 'chr17\t3000\t3000'
        allChrWrapper = new ChromosomeStatisticWrapper("all", 1)
        chrWrapper = new ChromosomeStatisticWrapper("chr17", 1)
        FileParameters fileParameters = new FileParameters()
        fileParameters.inputMode = Mode.EXOME
        fileParameters.bedFilePath = bedFile.absolutePath
        fileParameters.refGenMetaInfoFilePath = refGenMetaFile.absolutePath
        Parameters parameters = new Parameters()
        parameters.testMode = false
        parameters.minMeanBaseQuality = 0 // to make explicite, for code readability
        parameters.mappingQuality = 0 // to make explicite, for code readability
        worker = new SAMCountingStatisticWorker()
        worker.setFileParameters(fileParameters)
        worker.setParameters(parameters)
    }

    void tearDown() {
        allChrWrapper = null
        chrWrapper = null
        worker = null
        bedFile.delete()
        refGenMetaFile.delete()
    }

    /**
     * Test 1: reads off target (negative control)
     *
     * ref:     ______________   _____
     * target:            ___
     * reads:      _   _            _    _
     *
     * bedfile: chr17   1600    2000
     *
     * expected result:
     *      OnTargetMappedBases: 0
     *      AllMappedBases: |read1| + |read2| + |read3| = 300
     */
    void testNegativeControl() {

        ChromosomeStatisticWrapper chrQWrapper = new ChromosomeStatisticWrapper("chrQ", 1)
        ChromosomeStatisticWrapper chrNotMappedWrapper = new ChromosomeStatisticWrapper("chrNotMapped", 1)

        bedFile << 'chr17\t1600\t2000'
        refGenMetaFile << 'chrQ\t3000\t3000'
        worker.init()

        List records17 = []
        // create single end read1 attributes
        records17 << createRecord(1100, "100M", 100)
        // create single end read2 attributes
        records17 << createRecord(1300, "100M", 100)
        // create single read3 attributes for chrQ
        SAMRecord samRecordQ = createRecord(500, "100M", 100)
        // create single read4 which is unmapped
        SAMRecord samRecordNotMapped = new SAMRecord()
        samRecordNotMapped.setReadUnmappedFlag(true)

        records17.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.countBasesMapped(chrQWrapper.chromosome, samRecordQ)
        worker.countBasesMapped(chrNotMappedWrapper.chromosome, samRecordNotMapped)
        worker.processChromosomeAll([
            chrWrapper,
            chrQWrapper,
            chrNotMappedWrapper
        ], allChrWrapper)

        assertEquals(200, chrWrapper.chromosome.allBasesMapped)
        assertEquals(0, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(100, chrQWrapper.chromosome.allBasesMapped)
        assertEquals(0, chrQWrapper.chromosome.onTargetMappedBases)
        assertEquals(0, chrNotMappedWrapper.chromosome.allBasesMapped)
        assertEquals(0, chrNotMappedWrapper.chromosome.onTargetMappedBases)
        assertEquals(300, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(0, allChrWrapper.chromosome.onTargetMappedBases)
    }

    /**
     * Test 2: reads completely on target
     *
     * ref:     _______________
     * target:          ______
     * reads:            _  _
     *
     * bedfile: chr17   1000    1500
     *
     * expected result:
     *      OnTargetMappedBases: |read1| + |read2| = 200
     *      AllMappedBases: 200
     */
    void testReadsCompletelyOnTarget() {

        bedFile << 'chr17\t1000\t1500'
        worker.init()

        List records = []
        records << createRecord(1100, "100M", 100)
        records << createRecord(1300, "100M", 100)

        records.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.processChromosomeAll([chrWrapper], allChrWrapper)

        assertEquals(200, chrWrapper.chromosome.allBasesMapped)
        assertEquals(200, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(200, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(200, allChrWrapper.chromosome.onTargetMappedBases)
    }

    /**
     * Test 3: reads including gaps completely on target
     *
     * ref:     __________________
     * target:        ____________
     * reads:           _*_  _*__
     *
     * bedfile: chr17   1000    1500
     *
     * expected result:
     *      OnTargetMappedBases: |read1_gapfree| + |read2_gapfree| = 200
     *      AllMappedBases: 200
     */
    void testReadsIncludingGapsCompletelyOnTarget() {

        bedFile << 'chr17\t1000\t1500'
        worker.init()

        List records = []
        records << createRecord(1100, "10M10D90M", 110)
        records << createRecord(1250, "20M10D20M10D60M", 120)

        records.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.processChromosomeAll([chrWrapper], allChrWrapper)

        assertEquals(200, chrWrapper.chromosome.allBasesMapped)
        assertEquals(200, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(200, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(200, allChrWrapper.chromosome.onTargetMappedBases)
    }

    /**
     * Test 4.1: 1 split read on target
     *
     * ref:     __________________
     * target:        _________
     * reads:                 __
     *
     * bedfile: chr17   1000    1350
     *
     * expected result:
     *      OnTargetMappedBases: |read_overlap| = 51
     *      AllMappedBases: 100
     *
     */
    void testOneSplitReadOnTarget() {

        bedFile << 'chr17\t1000\t1350'
        worker.init()

        List records = []
        records << createRecord(1300, "100M", 100)

        records.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.processChromosomeAll([chrWrapper], allChrWrapper)

        assertEquals(100, chrWrapper.chromosome.allBasesMapped)
        assertEquals(51, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(100, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(51, allChrWrapper.chromosome.onTargetMappedBases)
    }

    /**
     * Test 4.2: 1 read completely on target
     *
     * ref:     __________________
     * target:        _________
     * reads:           __
     *
     * bedfile: chr17   1000    1350
     *
     * expected result:
     *      OnTargetMappedBases: |read| = 100
     *      AllMappedBases: 100
     *
     */
    void testOneReadCompletelyOnTarget() {

        bedFile << 'chr17\t1000\t1350'
        worker.init()

        List records = []
        records << createRecord(1100, "100M", 100)

        records.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.processChromosomeAll([chrWrapper], allChrWrapper)

        assertEquals(100, chrWrapper.chromosome.allBasesMapped)
        assertEquals(100, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(100, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(100, allChrWrapper.chromosome.onTargetMappedBases)
    }

    /**
     * Test 4.3: 1 read completely on target, 1 split read
     *
     * ref:     __________________
     * target:        _________
     * reads:           __    __
     *
     * bedfile: chr17   1000    1350
     *
     * expected result:
     *      OnTargetMappedBases: |read1| + |read2_overlap| = 100 + 51 = 151
     *      AllMappedBases: 200
     *
     */
    void testOneReadCompletelyOnTargetOneSplitRead() {

        bedFile << 'chr17\t1000\t1350'
        worker.init()

        List records = []
        records << createRecord(1100, "100M", 100)
        records << createRecord(1300, "100M", 100)

        records.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.processChromosomeAll([chrWrapper], allChrWrapper)

        assertEquals(200, chrWrapper.chromosome.allBasesMapped)
        assertEquals(151, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(200, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(151, allChrWrapper.chromosome.onTargetMappedBases)
    }

    /**
     * Test 5: split read including gaps
     *
     * ref:     __________________
     * target:        _________
     * reads:           _*_  _**__
     *
     * bedfile: chr17   1000    1305
     *
     * expected result:
     *      OnTargetMappedBases: |read1_gapfree| + |read2_gapfree_overlap| = 100 + 40 = 140
     *      AllMappedBases: 200
     */
    void testSplitReadIncludingGaps() {

        bedFile << 'chr17\t1000\t1305'
        worker.init()

        List records = []
        records << createRecord(1100, "10M10D90M", 110)
        records << createRecord(1250, "20M10D20M10D60M", 120)

        records.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.processChromosomeAll([chrWrapper], allChrWrapper)

        assertEquals(200, chrWrapper.chromosome.allBasesMapped)
        assertEquals(140, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(200, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(140, allChrWrapper.chromosome.onTargetMappedBases)
    }

    void testWgsCase() {
        FileParameters fileParameters = new FileParameters()
        fileParameters.inputMode = Mode.WGS
        worker.fileParameters = fileParameters
        worker.init()
        assertNull(worker.targetIntervals)

        ChromosomeStatisticWrapper chr1Wrapper = new ChromosomeStatisticWrapper("chr1", 5000)
        ChromosomeStatisticWrapper chr2Wrapper = new ChromosomeStatisticWrapper("chr2", 5000)
        List chr1Reads = []
        chr1Reads << createRecord(1100, "10M", 10)
        chr1Reads << createRecord(1300, "20M", 20)
        List chr2Reads = []
        chr2Reads << createRecord(1100, "30M", 30)
        chr2Reads << createRecord(1300, "40M", 40)

        chr1Reads.each { worker.countBasesMapped(chr1Wrapper.chromosome, it) }
        chr2Reads.each { worker.countBasesMapped(chr2Wrapper.chromosome, it) }
        worker.processChromosomeAll([chr1Wrapper, chr2Wrapper], allChrWrapper)

        assertEquals(30, chr1Wrapper.chromosome.allBasesMapped)
        assertEquals(0, chr1Wrapper.chromosome.onTargetMappedBases)
        assertEquals(70, chr2Wrapper.chromosome.allBasesMapped)
        assertEquals(0, chr2Wrapper.chromosome.onTargetMappedBases)
        assertEquals(100, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(0, allChrWrapper.chromosome.onTargetMappedBases)

        chr1Wrapper.chromosome.onTargetMappedBases = 1 // must not be summed up even if present
        allChrWrapper = new ChromosomeStatisticWrapper("all", 1)
        worker.processChromosomeAll([chr1Wrapper, chr2Wrapper], allChrWrapper)
        assertEquals(100, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(0, allChrWrapper.chromosome.onTargetMappedBases)
    }

    /**
     * ref:     17____________________   Q_______________
     * target:     _____  _____
     * reads:       __     __     __        ___     ___         ___  ___
     *
     */
    void testProcessChromosomeAllExome() {

        ChromosomeStatisticWrapper chrQWrapper = new ChromosomeStatisticWrapper("chrQ", 5000)
        ChromosomeStatisticWrapper chrStarWrapper = new ChromosomeStatisticWrapper("chr*", 5000)

        bedFile << 'chr17\t1000\t1300\n'
        bedFile << 'chr17\t1500\t2000'
        refGenMetaFile << 'chrQ\t1000\t3000' // not in bed file
        worker.init()

        List reads17 = []
        reads17 << createRecord(1100, "100M", 100)
        reads17 << createRecord(1600, "100M", 100)
        reads17 << createRecord(2100, "100M", 100)
        List readsQ = []
        readsQ << createRecord(500, "100M", 100)
        readsQ << createRecord(700, "100M", 100)
        List readsStar = []
        readsStar << createRecord(500, "100M", 100)
        readsStar << createRecord(700, "100M", 100)
        readsStar.each { it.setReadUnmappedFlag(true) }

        reads17.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        readsQ.each { worker.countBasesMapped(chrQWrapper.chromosome, it) }
        readsStar.each { worker.countBasesMapped(chrStarWrapper.chromosome, it) }

        worker.processChromosomeAll([chrWrapper, chrQWrapper, chrStarWrapper], allChrWrapper)

        assertEquals(300, chrWrapper.chromosome.allBasesMapped)
        assertEquals(200, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(200, chrQWrapper.chromosome.allBasesMapped)
        assertEquals(0, chrQWrapper.chromosome.onTargetMappedBases)
        assertEquals(0, chrStarWrapper.chromosome.allBasesMapped)
        assertEquals(0, chrStarWrapper.chromosome.onTargetMappedBases)
        assertEquals(500, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(200, allChrWrapper.chromosome.onTargetMappedBases)
    }

    void testBaseFiltering() {

        bedFile << 'chr17\t1000\t1350'
        worker.init()

        worker.parameters.mappingQuality = 5
        worker.parameters.minMeanBaseQuality = 5
        // create records passing the filtering
        List records = []
        4.times { records << createRecord(1100 + it + 1, "1M", 1) }
        // modify all the created records so that non of them pass the filtering
        records[0].duplicateReadFlag = true
        records[1].mappingQuality = 1
        byte[] baseQualities = new byte[1]
        baseQualities[0] = 1
        records[2].baseQualities = baseQualities
        records[3].readUnmappedFlag = true
        // failed to create record with alignmentEnd == 0 in the reasonable time, skip this case;
        // the function calling this methid has been already tested:
        // 1) see tests for SAMCountingStatisticWorker.coverageQc in this class
        // 2) comparison tests agains result of running Roddy: see build.gradle: comparisonTest task

        records.each { worker.countBasesMapped(chrWrapper.chromosome, it) }
        worker.processChromosomeAll([chrWrapper], allChrWrapper)

        assertEquals(0, chrWrapper.chromosome.allBasesMapped)
        assertEquals(0, chrWrapper.chromosome.onTargetMappedBases)
        assertEquals(0, allChrWrapper.chromosome.allBasesMapped)
        assertEquals(0, allChrWrapper.chromosome.onTargetMappedBases)
    }

    // the created record pass all the filters
    // see also quality values from the setUp method in Parameters
    private SAMRecord createRecord(int start, String cigar, int numOfBases) {
        SAMRecord samRecord = new SAMRecord()
        samRecord.alignmentStart = start
        samRecord.cigarString = cigar
        samRecord.duplicateReadFlag = false
        samRecord.mappingQuality = 10
        samRecord.readUnmappedFlag = false
        byte[] baseQualities = new byte[numOfBases]
        for (int i = 0; i < numOfBases; i++) {
            baseQualities[i] = 9
        }
        samRecord.baseQualities = baseQualities
        return samRecord
    }
}
