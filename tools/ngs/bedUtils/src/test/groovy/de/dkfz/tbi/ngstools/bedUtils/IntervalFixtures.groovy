package de.dkfz.tbi.ngstools.bedUtils

/**
 * container for re-usable intervals.
 *
 * Includes intervals for all binary combinations, as defined by J.F. Allen in [1]
 * and because pictures say more than a thousand words: see also [2], figure 1
 * <p>
 * Naming in this class is derived from Allen
 * </p>
 *
 * <ol>
 *   <li>
 *     J. F. Allen, "Towards a general theory of action and time".<br>
 *     <i>Artificial Intelligence 23</i> (1984), 123-154.<br>
 *     <a href="http://www.cs.ucf.edu/~lboloni/Teaching/EEL6938_2007/papers/Allen-GeneralTheoryActionTime.pdf">pdf</a>
 *   </li>
 *   <li>
 *      Claudio S. Pinhanez <i>et al.</i>, "Interval Scripts: a Design Paradigm for Story-Based Interactive Systems",<br>
 *     <i>CHI 97 Electronic Publications: Papers</i>, 22-27 march 1997<br>
 *     <a href="http://www.sigchi.org/chi97/proceedings/paper/csp.htm#allen_intervals">html</a>
 *   </li>
 * </ol>
 */
class IntervalFixtures {

    /*
     * same intervals
     * ===
     * ===
     */
   /** [10,20], one new instance */
   private static final Interval equalA = new Interval(10, 20)
   /** [10,20], other new instance */
   private static final Interval equalB = new Interval(10, 20)


    /*
     * the intervals are fully seperate.
     * ====
     *        ====
     */
    /** [10,20] */
    private static final Interval beforeA = new Interval(10, 20)
    /** [31,40] */
    private static final Interval beforeB = new Interval(31, 40)
    /** [20,10] */
    private static final Interval beforeAFlipped = beforeA.flip()
    /** [40,31] */
    private static final Interval beforeBFlipped = beforeB.flip()

    /*
     * the intervals touch, but don't overlap.
     * (synonyms: abuts, meets, touches
     * ====
     *     ====
     */
    /** [10,20] */
    private static final Interval meetA = new Interval(10, 20)
    /** [21,40] */
    private static final Interval meetB = new Interval(21, 40)
    /** [20,10] */
    private static final Interval meetAFlipped = meetA.flip()
    /** [40,21] */
    private static final Interval meetBFlipped = meetB.flip()

    /*
     * overlapping intervals
     * =====
     *    =====
     */
    /** [0,30] */
    private static final Interval overlapA = new Interval(0, 30)
    /** [20,40] */
    private static final Interval overlapB = new Interval(20, 40)
    /** [30,0] */
    private static final Interval overlapAFlipped = overlapA.flip()
    /** [40,20] */
    private static final Interval overlapBFlipped = overlapB.flip()

    /*
     * intervals spanning one-another.
     * Allen's "During" case, but name changed because we don't deal with "time" here
     * =========
     *   ====
     */
    /** [0,40] */
    private static final Interval spanA = new Interval(0, 40)
    /** [10,20] */
    private static final Interval spanB = new Interval(10, 20)
    /** [40, 0] */
    private static final Interval spanAFlipped = spanA.flip()
    /** [20,10] */
    private static final Interval spanBFlipped = spanB.flip()

    /*
     * Intervals having the same Start/from
     * ======
     * ===
     */
    /** [0,40] */
    private static final Interval startA = new Interval(0, 40)
    /** [0,20] */
    private static final Interval startB = new Interval(0, 20)
    /** [40, 0] */
    private static final Interval startAFlipped = startA.flip()
    /** [20,0] */
    private static final Interval startBFlipped = startB.flip()

    /*
     * Intervals having the same ending/to
     * ======
     *    ===
     */
    /** [0,40] */
    private static final Interval finishA = new Interval(0, 40)
   /** [20,40] */
   private static final Interval finishB = new Interval(20, 40)
   /** [40, 0] */
   private static final Interval finishAFlipped = finishA.flip()
   /** [40,20] */
   private static final Interval finishBFlipped = finishB.flip()


    /** a single point, length one, [+10,+10] */
    private static final Interval positivePoint = new Interval(10, 10)
    /** a single point, length one, [-10,-10] */
    private static final Interval negativePoint = new Interval(-10, -10)

    /** [10,20] */
    private static final Interval positiveAscending = new Interval(10, 20)
    /** [20,10] */
    private static final Interval positiveDescending = positiveAscending.flip()

    /** [-20,-10] */
    private static final Interval negativeAscending = new Interval(-20, -10)
    /** [-10,-20] */
    private static final Interval negativeDescending = negativeAscending.flip()
    /** [-10,+10] */
    private static final Interval zeroSpanningAscending = new Interval(-10, 10)
    /** [+10,-10] */
    private static final Interval zeroSpanningDescending = zeroSpanningAscending.flip()
}
