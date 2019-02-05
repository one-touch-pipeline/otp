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

/**
 * Represents content of a bed file and provides
 * some operations on it
 * NOTE:
 * in a bed file the intervals are defined with the following system:
 * 0 based: start inclusive, end exclusive
 * Methods of this interface take intervals in the same system.
 */
interface TargetIntervals {

    /**
     * Returns number of bases defined by chromosome refSeqName, start, end
     * which overlap with one or more intervals defined in the bed file.
     * The method is always run on a set of sorted and merged intervals.
     * @param refSeqName - name of reference sequence name (e.g. chromosome)
     * @param start - start point in the given reference sequence
     * @param end - end point in the given reference sequence
     * @return the calculated sum (see description)
     */
    long getOverlappingBaseCount(String refSeqName, long start, long end)

    /**
     * @return true if the initial list of intervals used for initialization
     * an instance of this class contains overlapping intervals
     */
    boolean hasOverlappingIntervals()

    /**
     * @return sum of lengths of all intervals from the initial list of
     * intervals used for initialization instance of this class
     */
    long getBaseCount()

    /**
     * @return summed length of sorted and merged intervals
     */
    long getUniqueBaseCount()

    /**
     * @return A set of strings holding all the reference sequence names that are
     * present in the BedFile
     */
    Set<String> getReferenceSequenceNames()

    /**
     * @param refSeqName - name of a reference sequence
     * @return true if bedFile contains this reference sequence
     */
    boolean containsReference(String refSeqName)
}
