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

class TargetIntervalsImpl implements TargetIntervals {

    private Map<String, MergingIntervalCollection> intervalsPerSequence

    /**
     * cached value of baseCount
     */
    private long baseCount

    /**
     * cached value of uniqueBaseCount
     */
    private long uniqueBaseCount

    /**
     * Absolute path of the Bed file
     */
    private String bedFilePath

    /**
     * List of reference genome entry names used for validation
     */
    private List<String> referenceGenomeEntryNames

    /**
     * Initiates the class from the given bedFilePath.
     * The initiation steps:
     * - parse bedFile
     * - check if the content of the BedFile is valid, meaning that the entry names in the bedFile are contained in the referenceGenomeEntryNames
     * - calculation of basic statistics
     * - create interval tree on sorted and merged set of intervals
     * @param bedFilePath - path to the bed file
     */
    TargetIntervalsImpl(String bedFilePath, List<String> referenceGenomeEntryNames) {
        // bedFilePath checking
        assert bedFilePath != null : "the bedFilePath parameter can not be null"
        File bedFile = new File(bedFilePath)
        assert bedFile.canRead() : "can not read the provided bedFile: ${bedFilePath}"
        assert bedFile.size() > 0 : "the provided bedFile is empty: ${bedFilePath}"
        // referenceGenomeEntryNames
        assert referenceGenomeEntryNames != null : "the referenceGenomeEntryNames parameter can not be null"
        assert !referenceGenomeEntryNames.isEmpty() : "the referenceGenomeEntryNames parameter can not be empty"
        this.bedFilePath = bedFilePath
        this.referenceGenomeEntryNames = referenceGenomeEntryNames
        this.intervalsPerSequence = [:]
    }

    void createTreeMapFromBedFile() {
        Map<String, List<Interval>> map = parseBedFile(bedFilePath)
        validateBedFileContent(referenceGenomeEntryNames, map)
        baseCount = calculateBaseCount(map)
        createTrees(map)
        uniqueBaseCount = calculateBaseCount(intervalsPerSequence)
    }

    private Map<String, List<Interval>> parseBedFile(String bedFilePath) {
        assert bedFilePath != null : "The input of the method parseBedFile is null"
        assert !bedFilePath.isEmpty() : "The input of the method parseBedFile is empty"
        Map<String, List<Interval>> map = [:]
        // to ensure nothing changed between calling TargetIntervalsImpl(...) and parseBedFile(...)
        File bedFile = new File(bedFilePath)
        assert bedFile.canRead() : "can not read the provided bedFile: ${bedFilePath}"
        assert bedFile.size() > 0 : "the provided bedFile is empty: ${bedFilePath}"
        bedFile.eachLine { String line, long no ->
            line = line.trim()
            if (line.empty) {
                println "Warning: There is an empty line in ${bedFilePath}, line ${no}"
            } else {
                List<String> values = line.split('\t')
                // handle chr name
                String refSeqName = values[0]
                assert !refSeqName.isEmpty() : "chomosome name can not be empty in the bedFile: ${bedFilePath}"
                // handle start position
                String startCol = values[1]
                assert !startCol.isEmpty() : "start position can not be empty"
                long start = startCol as long
                assert start >= 0 : "The start point of the interval is out of range"
                // handle stop position
                String endCol = values[2]
                assert !endCol.isEmpty() : "stop position can not be empty"
                long end = endCol as long
                assert end >= 0 : "The end point of the interval is out of range"
                // handle creation of interval and insertion into map
                Interval interval = null
                if (start < end) {
                    interval = toInternalSystem(start, end)
                } else {
                    interval = toInternalSystem(end, start)
                    println "Warning: The interval ${interval} was in the wrong order, continuing with correct-ordered version"
                }
                if (map.containsKey(refSeqName)) {
                    map[refSeqName].add(interval)
                } else {
                    map.put(refSeqName, [interval])
                }
            }
        }
        return map
    }

    /**
     * Ensures that all reference genome entries in the bed file are included in the reference genome.
     *
     * @param refGenomeEntryNames, a list which contains all names of the reference genome entries
     * @param map, a map containing a list of all intervals for each reference genome entry
     */
    private void validateBedFileContent(List<String> refGenomeEntryNames, Map<String, List<Interval>> map) {
        assert refGenomeEntryNames != null : "The input of the method validateBedFileContent is null"
        assert !refGenomeEntryNames.isEmpty() : "The input of the method validateBedFileContent is empty"
        assert map != null : "The input of the method validateBedFileContent is null"
        assert !map.isEmpty() : "The input of the method validateBedFileContent is empty"
        map.keySet().each { String key ->
            if (!refGenomeEntryNames.contains(key)) {
                throw new AssertionError("The BED file references entry ${key}, which does not exist in the reference genome.")
            }
        }
    }

    private long calculateBaseCount(Map<String, ? extends Iterable<Interval>> intervalsPerRefSeq) {
        assert intervalsPerRefSeq != null : "The input of the method calculateBaseCount is null"

        return intervalsPerRefSeq*.value.collect { Iterable<Interval> refSeqIntervals ->
            refSeqIntervals.collect { interval ->
                interval.length()
            }.sum()
        }.sum()
    }

    /**
     * Initialises the internal trees for use.
     * <p>
     * Part of the lazy-loading functionality; see {@link #parseBedFileIfRequired()}
     * </p>
     */
    private void createTrees(Map<String, List<Interval>> map) {
        assert map != null : "The input of the method createTree is null"
        map.each { String refSeqName, List<Interval> intervalList ->
            // initialise our IntervalTree
            def intervalTree = new MergingIntervalCollection()
            intervalList.each { Interval interval ->
                intervalTree.add(interval)
            }
            intervalsPerSequence.put(refSeqName, intervalTree)
        }
    }

    long getOverlappingBaseCount(String refSeqName, long startPosition, long endPosition) {
        assert refSeqName != null : "refSeqName in method getOverlappingBaseCount is null"
        assert startPosition != null : "start in method getOverlappingBaseCount is null"
        assert endPosition != null : "end in method getOverlappingBaseCount is null"
        Interval internalCoordinateInterval = toInternalSystem(startPosition, endPosition)

        parseBedFileIfRequired()
        long overlappingBaseCount = 0
        if(intervalsPerSequence.containsKey(refSeqName)) {
            List<Interval> overlappingIntervals = this.getOverlappingIntervals(refSeqName, internalCoordinateInterval.from, internalCoordinateInterval.to)
            // calculate overlapping bases to target
            overlappingIntervals.each { Interval overlappingInterval ->
                Interval intersection = overlappingInterval.intersect(internalCoordinateInterval)
                overlappingBaseCount += intersection.length()
            }
        }
        return overlappingBaseCount
    }

    /**
     * Returns all interval's in the bed-file that are (wholly or partially) in the requested interval.
     */
    private List<Interval> getOverlappingIntervals(String refSeqName, long targetStart, long targetEnd) {
        parseBedFileIfRequired()

        // failfast if we have no valid chromosome to work on.
        if (!intervalsPerSequence.containsKey(refSeqName)) {
            return Collections.emptyList()
        }

        return intervalsPerSequence[refSeqName].getOverlappingIntervals(targetStart, targetEnd) as List
    }

    /**
     * Checks if the BedFile has already been parsed, if not, parses it.
     */
    private void parseBedFileIfRequired() {
        // Developer note: This is a primitive form of lazy-loading.
        // If you are looking at this as an example, please learn about the "@lazy" annotation instead!

        // interval-map doubles as a flag if BedFile has been parsed yet
        if (intervalsPerSequence.isEmpty()) {
            createTreeMapFromBedFile()
        }
    }

    long getUniqueBaseCount() {
        parseBedFileIfRequired()
        return uniqueBaseCount
    }

    long getBaseCount() {
        parseBedFileIfRequired()
        return baseCount
    }

    boolean hasOverlappingIntervals() {
        parseBedFileIfRequired()
        return baseCount > uniqueBaseCount
    }

    boolean containsReference(String refSeqName) {
        assert refSeqName != null : "The input of the method contains is null"
        assert !refSeqName.isEmpty() : "The input of the method containsReference is empty"
        parseBedFileIfRequired()
        return intervalsPerSequence.containsKey(refSeqName)
    }

    Set<String> getReferenceSequenceNames() {
        parseBedFileIfRequired()
        return intervalsPerSequence.keySet()
    }

    /**
     * This function converts from bed-file system into internal system
     * <p>
     * this class uses the following system:
     * base: 0, start: inclusive, end: inclusive
     * </p>
     * <p>
     * bed-utils differs in that it has the last coordinate <i>exclusive</i>.<br>
     * </p>
     */
    private static Interval toInternalSystem(long start, long end) {
        assert end != start : "end must not be equal start"
        assert end > start, "end must be more than start : but start = $start and end = $end"
        return new Interval(start, end-1)
    }
}
