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

class RealBedFileTest {

    TargetIntervalsImpl targetIntervalsImpl

    @Before
    void setUp() throws Exception {
        String bedFilePath = "./testdata/test.bed"
        List<String> referenceGenomeEntryNames = [
            "chr1", "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16",
            "chr17", "chr18", "chr19", "chr2", "chr20", "chr21", "chr22", "chr3",
            "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chrM", "chrX", "chrY",
        ]
        targetIntervalsImpl = new TargetIntervalsImpl(bedFilePath, referenceGenomeEntryNames)
        targetIntervalsImpl.parseBedFileIfRequired()
    }

    @After
    void tearDown() throws Exception {
        targetIntervalsImpl = null
    }

    /**
     * checking on basic statistics for bedFile
     *
     * The expected value comes from the bedUtils itself for consistency
     */
    @Test
    void testBasicStats() {
        assertEquals(39973, targetIntervalsImpl.baseCount)
        assertEquals(39973, targetIntervalsImpl.uniqueBaseCount)
    }

    /**
     * target before last interval which is (chr1, 626534, 627367)
     */
    @Test
    void testTargetBeforeLastInterval() {
        Interval target = new Interval(626400L, 626500L)
        List<Interval> overlappingIntervals = targetIntervalsImpl.getOverlappingIntervals("chr1", target.from, target.to)
        assertEquals(0, overlappingIntervals.size)
    }

    /**
     * target in last interval
     */
    @Test
    void testTargetInLastInterval() {
        Interval target = new Interval(626600L, 627300L)
        List<Interval> overlappingIntervals = targetIntervalsImpl.getOverlappingIntervals("chr1", target.from, target.to)
        assertEquals(1, overlappingIntervals.size)
    }

    /**
     * target beyond last interval
     */
    @Test
    void testTargetBeyondLastInterval() {
        Interval target = new Interval(627400L, 627500L)
        List<Interval> overlappingIntervals = targetIntervalsImpl.getOverlappingIntervals("chr1", target.from, target.to)
        assertEquals(0, overlappingIntervals.size)
    }
}
