package de.dkfz.tbi.ngstools.bedUtils

import groovy.transform.EqualsAndHashCode

import java.util.Map.Entry

/**
 * Contains a collection of non-overlapping intervals, efficiently searchable.
 * <p>
 * Intervals newly added to this class are merged into the already contained intervals.
 * </p>
 *
 */
@EqualsAndHashCode
class MergingIntervalCollection implements Iterable<Interval> {
    /**
     * contains an index of all intervals, linked to their start-indices.
     * <p>
     * Used for the work-around method {@link #getOverlappingIntervals(String, Long, Long)}
     * See also jira OTP-651 for more details on the bug worked around
     * </p>
     */
    private final NavigableMap<Long, Interval> beginnings
    /**
     * contains an index of all intervals, linked to their start-indices.
     * <p>
     * Used for the work-around method {@link #getOverlappingIntervals(String, Long, Long)}
     * See also jira OTP-651 for more details on the bug worked around
     * </p>
     */
    private final NavigableMap<Long, Interval> endings

    /**
     * Gets all intervals overlapping (but not abutting) a given interval.
     * <p>
     * Details:
     * In the case the target-region spans the contained intervals
     * <pre>
     * target:        V----------------V
     * query: --- +++++++   +++  ++++ +++++ -----
     *         1     2       3a   3b    4     5
     * </pre>
     * </p>
     * <p>
     * In the case the target region is spanned by the contained interval<br>
     * <pre>
     * target:      V--V
     * query: --- +++++++ -----
     *               6
     * </pre>
     * <ul>
     * <li>1+5 MUST NOT be included, they are wholly outside the requested interval
     * <li>2+4 MUST be included, they overlap even though their start(2) and end (4) are outside the target region
     * <li>3a+3b MUST be included, they are wholly inside the target interval
     * <li>6 MUST be returned in it's entirety, not just the intersection with the target region.
     *   (so, given an interval [1,10] and a target [3,5] we return the entire [1,10]-region, not just [3,5])
     * </ul>
     * </p>
     *
     * @see TreeMap
     * @see TreeMap#subMap(Object, boolean, Object, boolean)
     */
    public Set<Interval> getOverlappingIntervals(Interval interval) {
        // step 1: get all the intervals with beginnings inside the target region
        // This gets 3a+3b+4
        final NavigableMap<Long, Interval> overlappingBeginnings = beginnings.subMap(interval.lowEnd, true, interval.highEnd, true)

        // step 2: get all the intervals with endings inside the target region
        // This gets 2+3a+3b
        final NavigableMap<Long, Interval> overlappingEndings = endings.subMap(interval.lowEnd, true, interval.highEnd, true)

        // step 3: merge step 1+2 to get complete result-set
        final Set<Interval> result = [] as Set
        result.addAll(overlappingBeginnings.values())
        result.addAll(overlappingEndings.values())

        // step 4: handle case-6.
        // to find these cases we expand the target-interval.
        // if both the expanded-start and the expanded-end are the same interval, that interval is also overlapping.
        final Entry<Long, Interval> floorEntry = beginnings.floorEntry(interval.lowEnd)
        final Entry<Long, Interval> ceilingEntry = endings.ceilingEntry(interval.highEnd)
        if (floorEntry != null && ceilingEntry != null && floorEntry.value == ceilingEntry.value) {
            result.add(floorEntry.value)
        }

        return result
    }

    /**
     * passthrough method to {@link #getOverlappingIntervals(Interval)}
     *
     * @param targetStart inclusive, that is: any interval that includes this position is included (even if it is the last position of said interval)
     * @param targetEnd   inclusive, that is: any interval that includes this position is included (even if it is the starting position of said interval)
     */
    public Set<Interval> getOverlappingIntervals(long targetFrom, long targetTo) {
        return getOverlappingIntervals(new Interval(targetFrom, targetTo))
    }

    /**
     * passthrough method for {@link #add(long, long)}
     */
    public void add(Interval interval) {
        add(interval.from, interval.to)
    }

    /**
     * Adds+merges the specified interval.
     * <p>
     * Internally, the new interval will be merged into all already-contained overlapping intervals
     * (as determined by {@link Interval#abuts(Interval)}, {@link Interval#overlaps(Interval) and Interval#union(Interval)})
     * </p>
     * <p>
     * NB: When the tree contains inverted/flipped intervals, or the added interval itself is inverted,
     * the stored order is no longer guaranteed. To prevent this, use the {@link #add(Interval)} with {@link Interval#asAscending()}
     * </p>
     *
     * @see #add(Interval)
     */
    public void add(final long newFrom, final long newTo) {
        // grow range by 1 in both direction, to also find abutting intervals
        // this needs to take direction into account
        final long expandedFrom
        final long expandedTo
        if (newFrom<=newTo) { // ascending
            expandedFrom = newFrom-1
            expandedTo = newTo+1
        } else {
            expandedFrom = newFrom+1
            expandedTo = newTo-1
        }

        // use non-expanded, clean interval to start, in case we have no overlaps
        Interval newInterval = new Interval(newFrom, newTo)

        // find all intervals overlapping the new region (if any) using the expanded interval,
        // to also find abutting intervals.
        Set<Interval> overlaps = getOverlappingIntervals(expandedFrom, expandedTo)

        // merge all overlapping intervals + new one into one
        // while removing merged intervals from collection
        overlaps.each {Interval old ->
            newInterval = newInterval.union(old) // safe, because only overlapping intervals are iterated.
            beginnings.remove(old.from)
            endings.remove(old.to)
        }

        // finally, add the fully merged item into the collection
        beginnings.put(newInterval.from, newInterval)
        endings.put(newInterval.to, newInterval)
    }

    /**
     * @return an immutable view of the currently contained Intervals
     */
    public Collection<Interval> getValues() {
        return beginnings.values().asImmutable()
    }

    public Iterator<Interval> iterator() {
        return getValues().iterator()
    }

    public void clear() {
        beginnings.clear()
        endings.clear()
    }

    public MergingIntervalCollection() {
        beginnings = new TreeMap<Long, Interval>()
        endings = new TreeMap<Long, Interval>()
    }
}