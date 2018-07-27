package de.dkfz.tbi.ngstools.bedUtils

import static org.springframework.util.Assert.*

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
    public TargetIntervalsImpl(String bedFilePath, List<String> referenceGenomeEntryNames) {
        // bedFilePath checking
        notNull(bedFilePath, "the bedFilePath parameter can not be null")
        File bedFile = new File(bedFilePath)
        isTrue(bedFile.canRead(), "can not read the provided bedFile: ${bedFilePath}")
        isTrue(bedFile.size() > 0, "the provided bedFile is empty: ${bedFilePath}")
        // referenceGenomeEntryNames
        notNull(referenceGenomeEntryNames, "the referenceGenomeEntryNames parameter can not be null")
        isTrue(!referenceGenomeEntryNames.isEmpty(), "the referenceGenomeEntryNames parameter can not be empty")
        this.bedFilePath = bedFilePath
        this.referenceGenomeEntryNames = referenceGenomeEntryNames
        this.intervalsPerSequence = [:]
    }

    public void createTreeMapFromBedFile() {
        Map<String, List<Interval>> map = parseBedFile(bedFilePath)
        validateBedFileContent(referenceGenomeEntryNames, map)
        baseCount = calculateBaseCount(map)
        createTrees(map)
        uniqueBaseCount = calculateBaseCount(intervalsPerSequence)
    }

    private Map<String, List<Interval>> parseBedFile(String bedFilePath) {
        notNull(bedFilePath, "The input of the method parseBedFile is null")
        notEmpty(bedFilePath, "The input of the method parseBedFile is empty")
        Map<String, List<Interval>> map = [:]
        // to ensure nothing changed between calling TargetIntervalsImpl(...) and parseBedFile(...)
        File bedFile = new File(bedFilePath)
        isTrue(bedFile.canRead(), "can not read the provided bedFile: ${bedFilePath}")
        isTrue(bedFile.size() > 0, "the provided bedFile is empty: ${bedFilePath}")
        bedFile.eachLine { String line, long no ->
            line = line.trim()
            if (line.empty) {
                println "Warning: There is an empty line in ${bedFilePath}, line ${no}"
            } else {
                List<String> values = line.split('\t')
                // handle chr name
                String refSeqName = values[0]
                notEmpty (refSeqName, "chomosome name can not be empty in the bedFile: ${bedFilePath}")
                // handle start position
                String startCol = values[1]
                notEmpty (startCol, "start position can not be empty")
                long start = startCol as long
                isTrue(start >= 0, "The start point of the interval is out of range")
                // handle stop position
                String endCol = values[2]
                notEmpty (endCol, "stop position can not be empty")
                long end = endCol as long
                isTrue(end >= 0, "The end point of the interval is out of range")
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
        notNull(refGenomeEntryNames, "The input of the method validateBedFileContent is null")
        notEmpty(refGenomeEntryNames, "The input of the method validateBedFileContent is empty")
        notNull(map, "The input of the method validateBedFileContent is null")
        notEmpty(map, "The input of the method validateBedFileContent is empty")
        map.keySet().each { String key ->
            if (!refGenomeEntryNames.contains(key)) {
                throw new IllegalArgumentException("The BED file references entry ${key}, which does not exist in the reference genome.")
            }
        }
    }

    private long calculateBaseCount(Map<String, ? extends Iterable<Interval>> intervalsPerRefSeq) {
        notNull(intervalsPerRefSeq, "The input of the method calculateBaseCount is null")

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
        notNull(map, "The input of the method createTree is null")
        map.each { String refSeqName, List<Interval> intervalList ->
            // initialise our IntervalTree
            def intervalTree = new MergingIntervalCollection()
            intervalList.each { Interval interval ->
                intervalTree.add(interval)
            }
            intervalsPerSequence.put(refSeqName, intervalTree)
        }
    }

    public long getOverlappingBaseCount(String refSeqName, long startPosition, long endPosition) {
        notNull(refSeqName, "refSeqName in method getOverlappingBaseCount is null")
        notNull(startPosition, "start in method getOverlappingBaseCount is null")
        notNull(endPosition, "end in method getOverlappingBaseCount is null")
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

    public long getUniqueBaseCount() {
        parseBedFileIfRequired()
        return uniqueBaseCount
    }

    public long getBaseCount() {
        parseBedFileIfRequired()
        return baseCount
    }

    public boolean hasOverlappingIntervals() {
        parseBedFileIfRequired()
        return baseCount > uniqueBaseCount
    }

    public boolean containsReference(String refSeqName) {
        notNull(refSeqName, "The input of the method contains is null")
        notEmpty(refSeqName, "The input of the method containsReference is empty")
        parseBedFileIfRequired()
        return intervalsPerSequence.containsKey(refSeqName)
    }

    public Set<String> getReferenceSequenceNames() {
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
        isTrue(end != start, "end must not be equal start")
        isTrue(end > start, "end must be more than start, but start = $start and end = $end")
        return new Interval(start, end-1)
    }
}
