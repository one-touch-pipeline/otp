package de.dkfz.tbi.ngstools.bedUtils

import static org.springframework.util.Assert.*
import edu.stanford.nlp.util.*

class TargetIntervalsImpl implements TargetIntervals {

    /**
     * contains IntervalTree per reference sequence from
     * the initial list of intervals. Each IntervalTree
     * includes only unique intervals (result of merging
     * of overlapping intervals from the initial list of intervals)
     * for the corresponding reference sequence.
     */
    private Map<String, IntervalTree> treeMap

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
     * Unique map containing no overlapping intervals
     */
    Map<String, List<Interval>> uniqueMap

    /**
     * Initiates the class from the given bedFilePath.
     * The initiation steps:
     * - parse bedFile
     * - check if the content of the BedFile is valid, meaning that the entry names in the bedFile are contained in the referenceGenomeEntryNames
     * - calculation of basic statistics
     * - create interval tree on unique set of intervals
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
        this.treeMap = [:]
    }

    public void createTreeMapFromBedFile() {
        Map<String, List<Interval>> map = parseBedFile(bedFilePath)
        isTrue(validateBedFileContent(referenceGenomeEntryNames, map), "Validation of referenceGenomeEntryNames with bed file failed")
        baseCount = calculateBaseCount(map)
        Map<String, List<Interval>> uniqueMap = unique(map)
        uniqueBaseCount = calculateBaseCount(uniqueMap)
        createTree(uniqueMap)
        this.uniqueMap = uniqueMap
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
                if (start <= end) {
                    interval = new Interval(start, end - 1, 0)
                } else {
                    interval = new Interval(end, start - 1, 0) // Charles: "do it"
                    println "Warning: The interval ${interval} is in the wrong order"
                }
                if (map.containsKey(refSeqName)) {
                    map.get(refSeqName).add(interval)
                } else {
                    List<Interval> list = [interval]
                    map.put(refSeqName, list)
                }
            }
        }
        return map
    }
    /**
     * @param refGenomeEntryNames, a list which contains all names of the reference genome entries
     * @param map, a map containing a list of all intervals for each reference genome entry
     * @return true if all reference genome entries in the bed file are included in the reference genome
     */
    private boolean validateBedFileContent(List<String> refGenomeEntryNames, Map<String, List<Interval>> map) {
        notNull(refGenomeEntryNames, "The input of the method validateBedFileContent is null")
        notEmpty(refGenomeEntryNames, "The input of the method validateBedFileContent is empty")
        notNull(map, "The input of the method validateBedFileContent is null")
        notEmpty(map, "The input of the method validateBedFileContent is empty")
        return map.keySet().every { String key ->
            refGenomeEntryNames.contains(key)
        }
    }

    private Map<String, List<Interval>> unique(Map<String, List<Interval>> map) {
        notNull(map, "The input of the method unique is null")
        Map<String, List<Interval>> uniqueMap = [:]
        map.each { String refSeqName, List<Interval> intervalList ->
            Collections.sort(intervalList, IntervalComparator.INSTANCE)
        }
        map.each { String refSeqName, List<Interval> intervalList ->
            List<Interval> uniqueList = mergeOverlapping(intervalList)
            uniqueMap.put(refSeqName, uniqueList)
        }
        return uniqueMap
    }

    /**
     * Modified version of getUniqueIntervals() from class IntervalList of Picard
     */
    private List<Interval> mergeOverlapping(List<Interval> intervalList) {
        notNull(intervalList, "The input of the method mergeOverlapping is null")
        if (!intervalList || intervalList.size() == 1) {
            return intervalList
        }
        List<Interval> uniqueList = []
        Iterator iterator = intervalList.iterator()
        Interval previous = iterator.next()
        // the actual merging logic
        while (iterator.hasNext()) {
            Interval next = iterator.next()
            boolean abuts = (previous.getBegin() == next.getEnd() + 1 || next.getBegin() == previous.getEnd() + 1)
            if (previous.overlaps(next) || abuts) {
                previous = new Interval(previous.getBegin(), Math.max(previous.getEnd(), next.getEnd()), 0)
            } else {
                uniqueList.add(previous)
                previous = next
            }
        }
        // to store multiple overlaps and last entry
        if (previous != null) {
            uniqueList.add(previous)
        }
        return uniqueList
    }

    private long calculateBaseCount(Map<String, List<Interval>> map) {
        notNull(map, "The input of the method calculateBaseCount is null")
        long baseCount
        map.each { String refSeqName, List<Interval> intervalList ->
            intervalList.each { Interval interval ->
                baseCount += length(interval)
            }
        }
        return baseCount
    }

    private void createTree(Map<String, List<Interval>> map) {
        notNull(map, "The input of the method createTree is null")
        map.each { String refSeqName, List<Interval> intervalList ->
            IntervalTree tree = new IntervalTree()
            tree.addAll(intervalList)
            treeMap.put(refSeqName, tree)
        }
    }

    public long getOverlappingBaseCount(String refSeqName, long startPosition, long endPosition) {
        notNull(refSeqName, "refSeqName in method getOverlappingBaseCount is null")
        notNull(startPosition, "start in method getOverlappingBaseCount is null")
        notNull(endPosition, "end in method getOverlappingBaseCount is null")
        Map interval = toInternalSystem(startPosition, endPosition)
        long start = interval.start
        long end = interval.end
        parseBedFileIfRequired()
        long overlappingBaseCount = 0
        if(uniqueMap.containsKey(refSeqName)) {
            List<Interval> overlappingIntervals = this.getOverlappingIntervals(refSeqName, start, end)
            // calculate overlapping bases to target
            Interval target = new Interval(start, end, 0)
            overlappingIntervals.each { Interval overlappingInterval ->
                Interval intersection = overlappingInterval.intersect(target)
                overlappingBaseCount += length(intersection)
            }
        }
        return overlappingBaseCount
    }

    /**
     * workaround to faulty coreNLP library used, see OTP-651
     */
    private List<Interval> getOverlappingIntervals(String refSeqName, long start, long end) {
        parseBedFileIfRequired()
        List<Interval> overlappingIntervals = []
        // implementation based on simple list
        if(uniqueMap.containsKey(refSeqName)) {
            List<Interval> chrIntervalList = uniqueMap.get(refSeqName)
            // find overlapping intervals
            // we know the list is sorted and unique
            Iterator iterator = chrIntervalList.iterator()
            while(iterator.hasNext()) {
                Interval interval = iterator.next()
                // move tail of target along sorted intervals
                if(end >= interval.getBegin()) {
                    if(start > interval.getEnd()) {
                        continue
                    } else {
                        overlappingIntervals.add(interval)
                    }
                }
            }
        }
        return overlappingIntervals
    }

    /**
     * treeMap is flag if BedFile has been parsed and treeMap has been created
     */
    private void parseBedFileIfRequired() {
        if (treeMap.isEmpty()) {
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
        return treeMap.keySet().contains(refSeqName)
    }

    private long length(Interval interval) {
        notNull(interval, "The input of the method length is null")
        long length = interval.getEnd() - interval.getBegin() + 1
        return length
    }

    public Set<String> getReferenceSequenceNames() {
        parseBedFileIfRequired()
        return treeMap.keySet()
    }

    /**
     * utils take input in bed file coordinate system,
     * this class uses the following system:
     * base: 0, start: inclusive, end: inclusive
     * This function converts from bed-file system into
     * internal system
     *
     */
    private static Map toInternalSystem(long start, long end) {
        isTrue(start > 0, "start must be more than 0, but was $start")
        isTrue(end >= start, "end must be more or equal than start, but start = $start and end = $end")
        return [start: start - 1, end: end]
    }
}
