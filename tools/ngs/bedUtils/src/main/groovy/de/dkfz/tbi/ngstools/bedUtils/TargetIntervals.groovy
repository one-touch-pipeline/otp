package de.dkfz.tbi.ngstools.bedUtils

import edu.stanford.nlp.util.*

/**
 * Represents content of a bed file and provides
 * some operations on it
 * NOTE:
 * in a bed file the intervals are defined with the following system:
 * 0 based: start inclusive, end exclusive
 * Methods of this interface take intervals in the same system.
 *
 */
interface TargetIntervals {

    /**
     * Returns number of bases defined by chromosome refSeqName, start, end
     * which overlap with one or more intervals defined in the bed file.
     * The method is always run on a set of unique intervals.
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
     * @return sum of length of all intervals from the list of unique intervals
     * which is result of merging of overlapping intervals from the initial
     * list of intervals.
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
