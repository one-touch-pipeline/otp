package de.dkfz.tbi.ngstools.bedUtils

import edu.stanford.nlp.util.*
/**
 * Class implementing an comparator to compare (and thus sort)
 * Intervals as defined by Picard.
 *
 */
enum IntervalComparator implements Comparator<Interval> {

    INSTANCE

    /**
     * Modified comparator from "IntervalList" of Picard
     */
    @Override
    public int compare(Interval lhs, Interval rhs) {
        int retVal = lhs.getBegin() - rhs.getBegin()
        if (retVal == 0) {
            retVal = lhs.getEnd() - rhs.getEnd()
        }
        if (retVal == 0) {
            // in case lhs and rhs have exactly the same start and end positions they are not further compared
            // and thus are seen as equal
            retVal = 0
        }
        return retVal
    }
}
