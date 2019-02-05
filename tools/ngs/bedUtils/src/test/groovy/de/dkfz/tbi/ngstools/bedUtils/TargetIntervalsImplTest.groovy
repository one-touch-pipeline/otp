/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.ngstools.bedUtils

import static org.junit.Assert.*

import org.junit.*

class TargetIntervalsImplTest {

    TargetIntervalsImpl targetIntervalsImpl
    File file
    String bedFilePath
    String fileContent
    List<String> referenceGenomeEntryNames

    @Before
    void setUp() throws Exception {
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
            "chr5",
        ]
        targetIntervalsImpl = new TargetIntervalsImpl(bedFilePath, referenceGenomeEntryNames)
    }

    @After
    void tearDown() throws Exception {
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
            "chr1": [new Interval(0L, 100L)],
            "chr2": [new Interval(32L, 105L)],
            "chr3": [new Interval(10000000L, 249250621L)],
            "chr4": [new Interval(5L, 49l)],
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
            "chr1": [new Interval(0L, 100L), new Interval(150L, 300L)],
            "chr2": [new Interval(32L, 105L)],
            "chr3": [new Interval(10000000L, 249250621L)],
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        assertEquals(intervalListExp, intervalListAct)
    }

    @Test
    void testParseBedFileNormalOrderIntervalWrongOrder() {
        fileContent += "\nchr1\t301\t150"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0L, 100L), new Interval(150L, 300L)],
            "chr2": [new Interval(32L, 105L)],
            "chr3": [new Interval(10000000L, 249250621L)],
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        assertEquals(intervalListExp, intervalListAct)
    }

    @Test(expected = IllegalArgumentException)
    void testParseBedFileNormalOrderIntervalStartEqualsEnd() {
        fileContent += "\nchr1\t301\t301"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListExp = [
            "chr1": [new Interval(0L, 100L), new Interval(301L, 300L)],
            "chr2": [new Interval(32L, 105L)],
            "chr3": [new Interval(10000000L, 249250621L)],
        ]
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        assertEquals(intervalListExp, intervalListAct)
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
            "chr1": [new Interval(0L, 100L), new Interval(150L, 300L)],
            "chr2": [new Interval(32L, 105L)],
            "chr3": [new Interval(10000000L, 249250621L)],
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
            "chr1": [new Interval(0L, 100L), new Interval(150L, 300L)],
            "chr2": [new Interval(32L, 105L)],
            "chr3": [new Interval(10000000L, 249250621L)],
        ]
        targetIntervalsImpl.validateBedFileContent(referenceGenomeEntryNames, map)
    }

    @Test(expected = IllegalArgumentException)
    void testValidateBedFileContentCase4() {
        Map<String, List<Interval>> map = [
            "chr1": [new Interval(0L, 100L), new Interval(150L, 300L)],
            "chr2": [new Interval(32L, 105L)],
            "chr3": [new Interval(10000000L, 249250621L)],
            "chr6": [new Interval(100L, 200L)],
        ]
        targetIntervalsImpl.validateBedFileContent(referenceGenomeEntryNames, map)
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
     * testGetOverlappingBaseCount
     * 1. null - exception
     * 2. target interval covered by single interval - check value
     * 3. target interval partially covered by multiple intervals - check value
     * 4. target interval not covered at all, but chromosome is present - check value
     * 5. target interval not covered at all due chromosome is missing - check value
     */

    @Test(expected = IllegalArgumentException)
    void testGetOverlappingBaseCountCase1() {
        targetIntervalsImpl.getOverlappingBaseCount(null, 10L, 20L)
    }

    @Test
    void testGetOverlappingBaseCountCase2() {
        fileContent = "chr1\t100\t201"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long result = targetIntervalsImpl.getOverlappingBaseCount("chr1", 190L, 281L)
        assertEquals(11, result)
    }

    @Test
    void testGetOverlappingBaseCountCase3() {
        fileContent = "chr1\t100\t201\nchr1\t250\t351"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)

        List<Interval> overlaps = targetIntervalsImpl.getOverlappingIntervals("chr1", 149L, 300L)
        assertEquals(2, overlaps.size())
        assertTrue(overlaps.contains(new Interval(100, 200)))
        assertTrue(overlaps.contains(new Interval(250, 350)))

        long actualOverlappingBaseCount = targetIntervalsImpl.getOverlappingBaseCount("chr1", 149L, 300L)
        assertEquals(52 + 50, actualOverlappingBaseCount)
    }

    @Test
    void testGetOverlappingBaseCountCase4() {
        fileContent = "chr1\t100\t201\nchr1\t250\t351"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long result = targetIntervalsImpl.getOverlappingBaseCount("chr1", 500L, 600L)
        assertEquals(0, result)
    }

    @Test
    void testGetOverlappingBaseCountCase5() {
        fileContent = "chr1\t100\t201\nchr1\t250\t351"
        file.withWriter { out ->
            out.writeLine(fileContent)
        }
        Map<String, List<Interval>> intervalListAct = targetIntervalsImpl.parseBedFile(bedFilePath)
        long result = targetIntervalsImpl.getOverlappingBaseCount("chr2", 100L, 200L)
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
        Interval expected = new Interval(0, 10)
        Interval output = TargetIntervalsImpl.toInternalSystem(0, 11)
        assertEquals(expected.from, output.from)
        assertEquals(expected.to, output.to)
        expected = new Interval(1, 1)
        output = TargetIntervalsImpl.toInternalSystem(1, 2)
        assertEquals(expected.from, output.from)
        assertEquals(expected.to, output.to)
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
