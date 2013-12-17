package de.dkfz.tbi.ngstools.bedUtils

import static org.junit.Assert.*

import org.junit.*

import edu.stanford.nlp.util.*

class RealBedFileTest {

    TargetIntervalsImpl targetIntervalsImpl

    @Before
    public void setUp() throws Exception {
        String bedFilePath = "./testdata/Agilent4withoutUTRs_chr.bed"
        List<String> referenceGenomeEntryNames = [
            "chr1", "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16",
            "chr17", "chr18", "chr19", "chr2", "chr20", "chr21", "chr22", "chr3",
            "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chrM", "chrX", "chrY"
        ]
        targetIntervalsImpl = new TargetIntervalsImpl(bedFilePath, referenceGenomeEntryNames)
        targetIntervalsImpl.parseBedFileIfRequired()
    }

    @After
    public void tearDown() throws Exception {
        targetIntervalsImpl = null
    }

    /**
     * checking on basic statistics for bedFile
     *
     * The expected value comes from the bedUtils itself for consistency
     */
    @Test
    void testBasicStats() {
        assertEquals(51189318, targetIntervalsImpl.baseCount)
        assertEquals(51189318, targetIntervalsImpl.uniqueBaseCount)
    }

    /**
     * target before last interval which is (chr1, 249210699, 249212659)
     */
    @Test
    void testTargetBeforeLastInterval() {
        Interval target = new Interval(249210499L, 249210599L)
        List<Interval> overlappingIntervals = targetIntervalsImpl.getOverlappingIntervals("chr1", target.from, target.to)
        assertEquals(0, overlappingIntervals.size)
    }

    /**
     * target in last interval
     */
    @Test
    void testTargetInLastInterval() {
        Interval target = new Interval(249211000L, 249211100L)
        List<Interval> overlappingIntervals = targetIntervalsImpl.getOverlappingIntervals("chr1", target.from, target.to)
        assertEquals(1, overlappingIntervals.size)
    }

    /**
     * target beyond last interval
     */
    @Test
    void testTargetBeyondLastInterval() {
        Interval target = new Interval(249212800L, 249212900L)
        List<Interval> overlappingIntervals = targetIntervalsImpl.getOverlappingIntervals("chr1", target.from, target.to)
        assertEquals(0, overlappingIntervals.size)
    }
}
