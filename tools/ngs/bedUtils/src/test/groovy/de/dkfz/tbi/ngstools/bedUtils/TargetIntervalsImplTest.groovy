package de.dkfz.tbi.ngstools.bedUtils

import org.junit.*
import static org.junit.Assert.*

import edu.stanford.nlp.util.*

class TargetIntervalsImplTest {

    TargetIntervalsImpl targetIntervalsImpl
    File file
    String bedFilePath
    String fileContent
    List<String> referenceGenomeEntryNames

    @Before
    public void setUp() throws Exception {
        //creation of the different possible test cases
        bedFilePath = "/tmp/kitname.bed"
        fileContent = "chr1\t0\t101\nchr2\t32\t106\nchr3\t10000000\t249250622"
        file = new File(bedFilePath)
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        referenceGenomeEntryNames = [
            "chr1",
            "chr2",
            "chr3",
            "chr4",
            "chr5"
        ]
        targetIntervalsImpl = new TargetIntervalsImpl(bedFilePath, referenceGenomeEntryNames)
    }

    @After
    public void tearDown() throws Exception {
        targetIntervalsImpl = null
        bedFilePath = null
        file.delete()
    }

    /*
     * testTargetIntervalsImpl
     * 1. null values a) bedFilePath b) referenceGenomeEntryNames
     * 2. a) empty bedFilePath, b) not existing bedFile
     * 3. referenceGenomeEntryNames is empty
     * 4. existing bedFile, referenceGenomeEntryNames with entries, check values are set
     */
    @Test(expected = IllegalArgumentException)
    void testTargetIntervalsImplCase1a() {
        targetIntervalsImpl = new TargetIntervalsImpl(null, referenceGenomeEntryNames)
    }

    @Test(expected = IllegalArgumentException)
    void testTargetIntervalsImplCase1b() {
        targetIntervalsImpl = new TargetIntervalsImpl(bedFilePath, null)
    }

    @Test(expected = IllegalArgumentException)
    void testTargetIntervalsImplCase2a() {
        targetIntervalsImpl = new TargetIntervalsImpl(new String(""), referenceGenomeEntryNames)
    }

    @Test(expected = IllegalArgumentException)
    void testTargetIntervalsImplCase2b() {
        targetIntervalsImpl = new TargetIntervalsImpl(new String("/tmp/asdsas"), referenceGenomeEntryNames)
    }

    @Test(expected = IllegalArgumentException)
    void testTargetIntervalsImplCase3() {
        List<String> referenceGenomeEntryNames = []
        targetIntervalsImpl = new TargetIntervalsImpl(bedFilePath, referenceGenomeEntryNames)
    }

    @Test
    void testTargetIntervalsImplCase4() {
        targetIntervalsImpl = new TargetIntervalsImpl(bedFilePath, referenceGenomeEntryNames)
        assertTrue(targetIntervalsImpl.treeMap.isEmpty())
        assertEquals(targetIntervalsImpl.bedFilePath, bedFilePath)
        assertEquals(targetIntervalsImpl.referenceGenomeEntryNames, referenceGenomeEntryNames)
    }

    /*
     * testParseBedFile
     *  - three trivial cases are tested
     *  - multiple tests for checking of correct
     *    behaviour of parsing the bed file
     *  - parsed input is used to check correct behaviour
     *    of the unique() method
     */
    @Test(expected = IllegalArgumentException)
    void testParseBedFilePathEmpty() {
        bedFilePath = ""
        targetIntervalsImpl.parseBedFile(bedFilePath)
    }

    @Test(expected = IllegalArgumentException)
    void testParseBedFileInputNull() {
        bedFilePath = null
        targetIntervalsImpl.parseBedFile(bedFilePath)
    }

    @Test
    void testParseBedFileEmptyLine() {
        fileContent += "\n\nchr4\t5\t50"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 100l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)],
            "chr4": [new Interval(5l, 49l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        assertEquals(intervalListExp, intervalListAct)
    }

   /*
    *  ---
    *       ---
    * Two non overlapping intervals in correct order on chr1
    */
    @Test
    void testParseBedFileNormalOrder() {
        fileContent += "\nchr1\t150\t301"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 100l, 0), new Interval(150l, 300l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        assertEquals(intervalListExp, intervalListAct)
    }


   /*
    * ----
    *    ----
    * Two non-overlapping intervals but intervals which are next to each other
    */
    @Test
    void testParseBedFileNeighbourIntervals() {
        fileContent += "\nchr1\t101\t201"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 200l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertEquals(intervalListExp, intervalListActUnique)
    }

   /*
    * ----
    *   -----
    * Two overlapping intervals
    */
    @Test
    void testParseBedFileOverlappingIntervals() {
        fileContent += "\nchr1\t50\t151"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 150l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertEquals(intervalListExp, intervalListActUnique)
    }

   /*
    * -------------
    *     ------
    * An interval fully spanning another interval
    */
    @Test
    void testParseBedFileIncludedInterval() {
        fileContent += "\nchr1\t30\t81"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 100l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertEquals(intervalListExp, intervalListActUnique)
    }

   /*
    * -----
    *    9---5
    * One interval in correct order and one interval in reverse order which overlap
    */
    @Test
    void testParseBedFileWrongOrder() {
        fileContent += "\nchr4\t201\t90\nchr4\t50\t151"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 100l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)],
            "chr4": [new Interval(50l, 200l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertEquals(intervalListExp, intervalListActUnique)
    }

   /*
    * -
    *  -----
    *     ----
    *        ---
    * multiple partially overlapping intervals
    */
    @Test
    void testParseBedFileMultipleOverlaps() {
        fileContent += "\nchr1\t100\t201\nchr1\t180\t281\nchr1\t260\t361"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 360l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)],
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertEquals(intervalListExp, intervalListActUnique)
    }

   /*
    * -----
    *    ----
    *         ---
    * partially overlapping interval and one single interval
    */
    @Test
    void testParseBedFileOneOverlapOneSingle() {
        fileContent += "\nchr1\t50\t151\nchr1\t200\t301"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 150l, 0), new Interval(200l, 300l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)],
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertEquals(intervalListExp, intervalListActUnique)
    }

   /*
    *  testValidateBedFileContent
    *	1. refGenomeEntryNames = null - exception
    *	2. map = null - exception
    *	3. if all map.keyset elements are included in refGenomeEntryNames - true
    *	4. if one map.keyset element exists which is not in refGenomeEntryNames - false
    */
    @Test(expected = IllegalArgumentException)
    void testValidateBedFileContentCase1() {
        Map<String, List<Interval>> map = [
            "chr1": [new Interval(0l, 100l, 0), new Interval(150l, 300l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
        targetIntervalsImpl.validateBedFileContent(null, map)
    }

    @Test(expected = IllegalArgumentException)
    void testValidateBedFileContentCase2() {
        targetIntervalsImpl.validateBedFileContent(referenceGenomeEntryNames, null)
    }

    @Test
    void testValidateBedFileContentCase3() {
        Map<String, List<Interval>> map = [
            "chr1": [new Interval(0l, 100l, 0), new Interval(150l, 300l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
       assertTrue(targetIntervalsImpl.validateBedFileContent(referenceGenomeEntryNames, map))
    }

    @Test
    void testValidateBedFileContentCase4() {
        Map<String, List<Interval>> map = [
            "chr1": [new Interval(0l, 100l, 0), new Interval(150l, 300l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)],
            "chr6": [new Interval(100l, 200l, 0)]
        ]
        assertFalse(targetIntervalsImpl.validateBedFileContent(referenceGenomeEntryNames, map))
    }

    /*
     * testUnique
     * different cases to check:
     * 1. if input map null - exception
     * 2. if input map has no overlapping intervals - maps shall be the same
     * 3. if input map has overlap - maps shall be different
     */
    @Test(expected = IllegalArgumentException)
    void testUniqueCase1() {
        targetIntervalsImpl.unique(null)
    }

    @Test
    void testUniqueCase2() {
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 100l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertEquals(intervalListExp, intervalListActUnique)
    }

    @Test
    void testUniqueCase3() {
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0l, 100l, 0), new Interval(50l, 150l, 0)],
            "chr2": [new Interval(32l, 105l, 0)],
            "chr3": [new Interval(10000000l, 249250621l, 0)]
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        Map<String, List<Interval>> intervalListActUnique = targetIntervalsImpl.unique(intervalListAct)
        assertFalse(intervalListExp == intervalListActUnique)
    }

    /*
     * different cases to be checked:
     * 1. intervalList is null
     * 2. 2 overlapping interval - check if merged interval is correct
     * 3. 2 non-overlapping intervals - check that nothing happened
     * other cases are checked via testParseBedFile... methods
     */
    @Test(expected = IllegalArgumentException)
    void testMergeOverlappingCase1() {
        targetIntervalsImpl.mergeOverlapping(null)
    }

    @Test
    void testMergeOverlappingCase2() {
        List<Interval> intervalList = [new Interval(0l, 100l, 0), new Interval(50l, 150l, 0)]
        List<Interval> intervalListExp = [new Interval(0l, 150l, 0)]
        List<Interval> intervalListAct = targetIntervalsImpl.mergeOverlapping(intervalList)
        assertEquals(intervalListExp, intervalListAct)
    }

    @Test
    void testMergeOverlappingCase3() {
        List<Interval> intervalList = [new Interval(0l, 100l, 0), new Interval(200l, 300l, 0)]
        List<Interval> intervalListExp = [new Interval(0l, 100l, 0), new Interval(200l, 300l, 0)]
        List<Interval> intervalListAct = targetIntervalsImpl.mergeOverlapping(intervalList)
        assertEquals(intervalListExp, intervalListAct)
    }

    /*
     * testCalculateBaseCount
     * 1. null value - exception
     * 2. non overlaps - check value
     * 3. overlaps - check value
     */
    @Test(expected = IllegalArgumentException)
    void testCalculateBaseCountCase1() {
        targetIntervalsImpl.calculateBaseCount(null)
    }

    @Test
    void testCalculateBaseCountCase2() {
        fileContent = "chr1\t1\t101\nchr2\t1000\t3001\nchr3\t10\t21"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long baseCountActual = targetIntervalsImpl.calculateBaseCount(intervalListAct)
        long baseCountExpected = 100 + 2001 + 11
        assertEquals(baseCountActual, baseCountExpected)
    }

    @Test
    void testCalculateBaseCountCase3() {
        fileContent = "chr1\t1\t101\nchr1\t80\t181"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long baseCountActual = targetIntervalsImpl.calculateBaseCount(intervalListAct)
        long baseCountExpected = 100 + 101
        assertEquals(baseCountActual, baseCountExpected)
    }

    /*
     * testCreateTree
     * 1. null
     * 2. number of trees in map shall equal number of chromosomes AND
     *    each tree shall have correct number of intervals
     */
    @Test(expected = IllegalArgumentException)
    void testCreateTreeCase1() {
        targetIntervalsImpl.createTree(null)
    }

    @Test
    void testCreateTreeCase2() {
        Map<String, List<Interval>> map = [
            "chr1": [new Interval(0l, 100l, 0), new Interval(200l, 300l, 0)],
            "chr2": [new Interval(100l, 200l, 0)],
            "chr3": [new Interval(100l, 200l, 0)]
        ]
        targetIntervalsImpl.createTree(map)
        assertEquals(3, targetIntervalsImpl.treeMap.size())
        assertEquals(2, targetIntervalsImpl.treeMap.get("chr1").size())
        assertEquals(1, targetIntervalsImpl.treeMap.get("chr2").size())
        assertEquals(1, targetIntervalsImpl.treeMap.get("chr3").size())
    }

    /*
     * testGetOverlappingBaseCount
     * 1. null - exception
     * 2. target interval covered by single interval - check value
     * 3. target interval partially covered by multiple intervals - check value
     * 4. target interval not covered at all, but chromosome is present - check value
     * 5. target interval not covered at all due chromosome is missing - check value
     */

    @Test(expected = IllegalArgumentException)
    void testGetOverlappingBaseCountCase1() {
        targetIntervalsImpl.getOverlappingBaseCount(null, 10l, 20l)
    }

    @Test
    void testGetOverlappingBaseCountCase2() {
        fileContent = "chr1\t100\t201"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long result = targetIntervalsImpl.getOverlappingBaseCount("chr1", 190l, 281l)
        assertEquals(11, result)
    }

    @Test
    void testGetOverlappingBaseCountCase3() {
        fileContent = "chr1\t100\t201\nchr1\t250\t351"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long result = targetIntervalsImpl.getOverlappingBaseCount("chr1", 150l, 301l)
        assertEquals(51 + 51, result)
    }

    @Test
    void testGetOverlappingBaseCountCase4() {
        fileContent = "chr1\t100\t201\nchr1\t250\t351"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long result = targetIntervalsImpl.getOverlappingBaseCount("chr1", 500l, 600l)
        assertEquals(0, result)
    }

    @Test
    void testGetOverlappingBaseCountCase5() {
        fileContent = "chr1\t100\t201\nchr1\t250\t351"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long result = targetIntervalsImpl.getOverlappingBaseCount("chr2", 100l, 200l)
        assertEquals(0, result)
    }

    /*
     * testGetUniqueBaseCount
     */
    @Test
    void testGetUniqueBaseCount() {
        fileContent = "chr1\t150\t301\nchr1\t250\t351"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        targetIntervalsImpl.parseBedFile(bedFilePath)
        assertEquals(201, targetIntervalsImpl.getUniqueBaseCount())
    }

    /*
     * testGetBaseCount
     */
    @Test
    void testGetBaseCount() {
        fileContent = "chr1\t150\t301"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        targetIntervalsImpl.parseBedFile(bedFilePath)
        assertEquals(151, targetIntervalsImpl.getBaseCount())
    }

   /*
    * testHasOverlappingIntervals
    * 1. bed file contains no overlapping intervals - false
    * 2. bed file contains overlaps - true
    */

    @Test
    void testHasOverlappingIntervalsCase1() {
        fileContent = "chr1\t150\t301\nchr1\t400\t501"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        targetIntervalsImpl.parseBedFile(bedFilePath)
        assertFalse(targetIntervalsImpl.hasOverlappingIntervals())
    }

    @Test
    void testHasOverlappingIntervalsCase2() {
        fileContent = "chr1\t150\t301\nchr1\t200\t501"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        targetIntervalsImpl.parseBedFile(bedFilePath)
        assertTrue(targetIntervalsImpl.hasOverlappingIntervals())
    }

    /*
     * testContainsReference
     * 1. null
     * 2. reference name in tree (bedFile) - true
     * 3. reference name not in tree - false
     */
    @Test(expected = IllegalArgumentException)
    void testContainsReferenceCase1() {
        targetIntervalsImpl.containsReference(null)
    }

    @Test
    void testContainsReferenceCase2() {
        assertTrue(targetIntervalsImpl.containsReference("chr1"))
    }

    @Test
    void testContainsReferenceCase3() {
        assertFalse(targetIntervalsImpl.containsReference("chr5"))
    }

    /*
     * testLength
     */
    @Test(expected = IllegalArgumentException)
    void testLengthNull() {
        targetIntervalsImpl.length(null)
    }

    /*
     * this test has to fail, since the start and the end have to be specified (edu.stanford.nlp.util Interval API)
     */
    @Test(expected = NullPointerException)
    void testLengthNoLengthInIntervalBoth() {
        new Interval(null, null, 0)
    }

    /*
     * this test has to fail, since the start and the end have to be specified (edu.stanford.nlp.util Interval API)
     */
    @Test(expected = NullPointerException)
    void testLengthNoLengthInIntervalA() {
        new Interval(null, 3l, 0)
    }

    /*
     * this test has to fail, since the start and the end have to be specified (edu.stanford.nlp.util Interval API)
     */
    @Test(expected = NullPointerException)
    void testLengthNoLengthInIntervalB() {
        new Interval(3l, null, 0)
    }

    @Test
    void testLengthEqualStartAndEnd() {
        Interval interval = new Interval(4l, 4l, 0)
        assertEquals(1, targetIntervalsImpl.length(interval))
    }

    @Test
    void testLengthStartAndEndNextToEachOther() {
        Interval interval = new Interval(4l, 5l, 0)
        assertEquals(2, targetIntervalsImpl.length(interval))
    }

    /*
     * this test has to fail, since the start and the end have to be specified (edu.stanford.nlp.util Interval API)
     */
    @Test(expected = IllegalArgumentException)
    void testLengthIsNegative() {
        new Interval(34l, 6l, 0)
    }

    @Test
    void testLengthIsVeryBig() {
        Interval interval = new Interval(0L, 9223372036854775800L, 0)
        assertEquals(9223372036854775801L, targetIntervalsImpl.length(interval))
    }

    /*
     * testCheckBedFileParsed
     */
    @Test
    void testCheckBedFileParsed() {
        targetIntervalsImpl.treeMap = [:]
        targetIntervalsImpl.parseBedFileIfRequired()
        assertTrue(!targetIntervalsImpl.treeMap.isEmpty())
    }

    /*
     * testCreateTreeMapFromBedFile
     */
    @Test
    void testCreateTreeMapFromBedFile() {
        fileContent = "chr1\t100\t201\nchr1\t300\t401\nchr2\t100\t201"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        targetIntervalsImpl.createTreeMapFromBedFile()
        assertEquals(2, targetIntervalsImpl.treeMap.size())

        Interval interval
        // check input to output for chr1
        IntervalTree treeChr1 = targetIntervalsImpl.treeMap.get("chr1")
        assertTrue(treeChr1.contains(new Interval(100l, 200l, 0)))
        assertTrue(treeChr1.contains(new Interval(300l, 400l, 0)))
        // should not be in the tree
        assertFalse(treeChr1.contains(new Interval(800l, 900l, 0)))
        // check input to output for chr2
        IntervalTree treeChr2 = targetIntervalsImpl.treeMap.get("chr2")
        assertTrue(treeChr2.contains(new Interval(100l, 200l, 0)))
    }

    /*
     * testGetReferenceSequenceNames
     */
    @Test
    void testGetReferenceSequenceNames() {
        targetIntervalsImpl.createTreeMapFromBedFile()
        Set<String> referenceSequenceNames = targetIntervalsImpl.getReferenceSequenceNames()
        assertTrue(referenceSequenceNames.contains("chr1"))
        assertTrue(referenceSequenceNames.contains("chr2"))
        assertTrue(referenceSequenceNames.contains("chr3"))
        assertFalse(referenceSequenceNames.contains("chr4"))
        assertEquals(3, referenceSequenceNames.size())
    }

    @Test
    void testToInternalSystemCorrect() {
        Interval expected = new Interval(0, 10, 0)
        Interval output = TargetIntervalsImpl.toInternalSystem(0, 11)
        assertEquals(expected.begin, output.begin)
        assertEquals(expected.end, output.end)
        expected = new Interval(1, 1, 0)
        output = TargetIntervalsImpl.toInternalSystem(1, 2)
        assertEquals(expected.begin, output.begin)
        assertEquals(expected.end, output.end)
    }

    @Test(expected = IllegalArgumentException)
    void testToInternalSystemStartEqualsEnd() {
        TargetIntervalsImpl.toInternalSystem(10, 10)
    }

    @Test(expected = IllegalArgumentException)
    void testToInternalSystemEndMoreThanStart() {
        TargetIntervalsImpl.toInternalSystem(10, 1)
    }

    @Test(expected = IllegalArgumentException)
    void testStartEqualsEnd() {
        fileContent = "chr4\t50\t50"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        targetIntervalsImpl.parseBedFile(bedFilePath)
    }
}
