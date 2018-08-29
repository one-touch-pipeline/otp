package de.dkfz.tbi.ngstools.bedUtils

import groovy.transform.EqualsAndHashCode

/**
 * Describes an interval defined by a start-index and an end-index, both inclusive.
 *
 * Note that there is no guarantee that {@link #from} <= {@link #to}; "negative" intervals are possible.
 * Both start and end are final, to make this class effectively immutable.
 */
@EqualsAndHashCode(includeFields=true, excludes="lowEnd, highEnd")
class Interval implements Comparable<Interval>{
    Interval(long from, long to) {
        this.from = from
        this.to   = to
    }

    final long from
    final long to

    /** the math.min lowest of [from,to] */
    public long getLowEnd() {
        return Math.min(from, to)
    }

    /** the math.max highest of [from,to] */
    public long getHighEnd() {
        return Math.max(from, to)
    }

    /** this interval from <= to? */
    public boolean isAscending() {
        return from <= to
    }

    /**
     * returns this interval, but always in ascending order, guaranteeing that for the returned <code>Interval, from&lt;=to</code>
     */
    public Interval asAscending() {
        if (isAscending()) {
            return this
        } else {
            return this.flip()
        }
    }

    /**
     * @return the same interval, but from/to flipped
     */
    public Interval flip() {
        return new Interval(to, from)
    }

    /** does this interval share at least one whole index with the specified other? */
    public boolean overlaps(Interval other) {
        // the intervals don't overlap if either
        // - we are completely before them or
        // - they are completely before us
        // in all other cases, we overlap, or touch, at least one position
        // Note that we (safely) ignore direction in this comparison, by treating both in ascending order
        return !(this.highEnd < other.lowEnd || other.highEnd < this.lowEnd)
    }

    /**
     * Are these intervals directly touching ends? (regardless of direction)
     * <p>
     * so both [10,20] and [20,10] abut all of [21,30], [30,21], [0,9], [9,0]
     * </p>
     *
     */
    public boolean abuts(Interval other) {
        // compare both in ascending order, so we can ignore the direction in this comparison
        return (this.highEnd+1 == other.lowEnd || other.highEnd+1 == this.lowEnd)
    }

    /**
     * Gets the smallest section, inclusive existing in both intervals.
     *
     * @param other the other interval.
     * @return the overlapping section, in the same direction as the original, or <code>null</code> if no overlap
     */
    public Interval intersect(Interval other) {
        // fail fast if no overlap at all
        if (!this.overlaps(other)) {
            return null
        }

        // if overlap, get intersection.
        // The simplest way to get this is the biggest-start and the smallest end
        // (which works, because we use the values in ascending direction, and worry about direction below)
        long newLo = Math.max(this.lowEnd, other.lowEnd)
        long newHi = Math.min(this.highEnd, other.highEnd)

        // sort intersection in same direction as this object
        if (this.isAscending()) {
            return new Interval(newLo, newHi)
        } else {
            return new Interval(newHi, newLo)
        }
    }

    /**
     * @return the combined interval spanning both intervals, in the same direction as this interval.
     *  Or <code>null</code> when the two don't touch or overlap
     * @see #abuts(Interval)
     * @see #overlaps(Interval)
     */
    public Interval union(Interval joinCandidate) {
        if (this.abuts(joinCandidate) || this.overlaps(joinCandidate)) {
            // return merged interval, in same direction as this object
            long newLowEnd = Math.min(this.lowEnd, joinCandidate.lowEnd)
            long newHighEnd = Math.max(this.highEnd, joinCandidate.highEnd)

            if (this.isAscending()) {
                // return ascending, low-->high
                return new Interval(newLowEnd, newHighEnd)
            } else {
                // return descending, high-->low
                return new Interval(newHighEnd, newLowEnd)
            }
        }
        // If we don't touch or overlap, return garbage
        return null
    }

    /**
     * compares this interval to another, first by beginnings, then by endings.
     */
    @Override
    public int compareTo(Interval other) {
        // compare beginnings
        int retVal = from <=> other.from

        // if beginnins equal, compare by end
        if (retVal == 0) {
            retVal = to <=> other.to
        }
        return retVal
    }

    /**
     * the lenght of the interval, inclusive
     * so <code>new Interval(3,3).length()</code> = 1
     */
    public long length() {
        return highEnd - lowEnd + 1
    }

    @Override
    public String toString() {
        return "Interval(${from},${to})"
    }
}

