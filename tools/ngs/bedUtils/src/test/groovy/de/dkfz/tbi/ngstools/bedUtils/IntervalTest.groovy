package de.dkfz.tbi.ngstools.bedUtils

import static de.dkfz.tbi.ngstools.bedUtils.IntervalFixtures.*
import static org.junit.Assert.*

import org.junit.*

/**
 * This tests {@link Interval}.
 *
 * Notably, all the binary methods on Interval are tested using the full set of possible combinations,
 * as provided by {@link IntervalFixtures}
 * <ul>
 * <li> {@link Interval#abuts(Interval)}
 * <li> {@link Interval#overlaps(Interval)}
 * <li> {@link Interval#intersect(Interval)}
 * <li> {@link Interval#compareTo(Interval)}
 * <li> {@link Interval#union(Interval)}
 *
 */
class IntervalTest {

    List<Interval> intervalList

    @Before
    public void setUp() throws Exception {
        intervalList = []
    }

    @After
    public void tearDown() throws Exception {
        intervalList = null
    }

    /*
     * Tests for the compare() method
     *
     * 1. start of interval A before start of interval B; exp.: A, B
     * 2. start of interval B before start of interval A; exp.: B, A
     * 3. start of interval A and B same, end of interval B > end of interval A; exp.: A, B
     * 4. start of interval A and B same, end of interval A > end of interval B; exp.: B, A
     * 5. start and end of interval A and B are exactly the same; exp.: A, B
     * 6. A and B don't overlap at all
     * 7. A overarches B
     *
     * order in original list is always A, B
     */

    /**
     * equal
     * <pre>
     * A ===
     * B ===
     * </pre>
     */
    @Test
    void testCompareEqualInterval() {
        assertTrue(positivePoint.compareTo(positivePoint) == 0)
        assertTrue(negativePoint.compareTo(negativePoint) == 0)
        assertTrue(equalA.compareTo(equalA) == 0) // same object
        assertTrue(equalA.compareTo(equalB) == 0) // different object, same interval
    }

    /**
     * before
     * <pre>
     * A ===
     * B      ===
     * </pre>
     */
    @Test
    void testCompareBeforeInterval() {
        assertTrue(beforeA.compareTo(beforeB)               < 0)
        assertTrue(beforeA.compareTo(beforeBFlipped)        < 0)
        assertTrue(beforeB.compareTo(beforeA)               > 0)
        assertTrue(beforeB.compareTo(beforeAFlipped)        > 0)
        assertTrue(beforeAFlipped.compareTo(beforeB)        < 0)
        assertTrue(beforeAFlipped.compareTo(beforeBFlipped) < 0)
        assertTrue(beforeBFlipped.compareTo(beforeA)        > 0)
        assertTrue(beforeBFlipped.compareTo(beforeAFlipped) > 0)
    }

    /**
     * meeting
     * <pre>
     * A ===
     * B    ===
     * </pre>
     */
    @Test
    void testCompareMeetingInterval() {
        assertTrue(meetA.compareTo(meetB)               < 0)
        assertTrue(meetA.compareTo(meetBFlipped)        < 0)
        assertTrue(meetB.compareTo(meetA)               > 0)
        assertTrue(meetB.compareTo(meetAFlipped)        > 0)
        assertTrue(meetAFlipped.compareTo(meetB)        < 0)
        assertTrue(meetAFlipped.compareTo(meetBFlipped) < 0)
        assertTrue(meetBFlipped.compareTo(meetA)        > 0)
        assertTrue(meetBFlipped.compareTo(meetAFlipped) > 0)
    }

    /**
     * overlapping
     * <pre>
     * A ====
     * B    ====
     * </pre>
     */
    @Test
    void testCompareOverlappingInterval() {
        assertTrue(overlapA.compareTo(overlapB)               < 0)
        assertTrue(overlapA.compareTo(overlapBFlipped)        < 0)
        assertTrue(overlapB.compareTo(overlapA)               > 0)
        assertTrue(overlapB.compareTo(overlapAFlipped)        < 0)
        assertTrue(overlapAFlipped.compareTo(overlapB)        > 0)
        assertTrue(overlapAFlipped.compareTo(overlapBFlipped) < 0)
        assertTrue(overlapBFlipped.compareTo(overlapA)        > 0)
        assertTrue(overlapBFlipped.compareTo(overlapAFlipped) > 0)
    }

    /**
     * one contains other
     * <pre>
     * A =======
     * B   ===
     * </pre>
     */
    @Test
    void testCompareOverlapInterval() {
        assertTrue(spanA.compareTo(spanB)               < 0)
        assertTrue(spanA.compareTo(spanBFlipped)        < 0)
        assertTrue(spanB.compareTo(spanA)               > 0)
        assertTrue(spanB.compareTo(spanAFlipped)        < 0)
        assertTrue(spanAFlipped.compareTo(spanB)        > 0)
        assertTrue(spanAFlipped.compareTo(spanBFlipped) > 0)
        assertTrue(spanBFlipped.compareTo(spanA)        > 0)
        assertTrue(spanBFlipped.compareTo(spanAFlipped) < 0)
    }

    /**
     * start same
     * <pre>
     * A ======
     * B ===
     * </pre>
     */
    @Test
    void testCompareSameStartingInterval() {
        assertTrue(startA.compareTo(startB)               > 0)
        assertTrue(startA.compareTo(startBFlipped)        < 0)
        assertTrue(startB.compareTo(startA)               < 0)
        assertTrue(startB.compareTo(startAFlipped)        < 0)
        assertTrue(startAFlipped.compareTo(startB)        > 0)
        assertTrue(startAFlipped.compareTo(startBFlipped) > 0)
        assertTrue(startBFlipped.compareTo(startA)        > 0)
        assertTrue(startBFlipped.compareTo(startAFlipped) < 0)
    }

    /**
     * ending same
     * <pre>
     * A ======
     * B    ===
     * </pre>
     */
    @Test
    void testCompareSameEndingInterval() {
        assertTrue(finishA.compareTo(finishB)               < 0)
        assertTrue(finishA.compareTo(finishBFlipped)        < 0)
        assertTrue(finishB.compareTo(finishA)               > 0)
        assertTrue(finishB.compareTo(finishAFlipped)        < 0)
        assertTrue(finishAFlipped.compareTo(finishB)        > 0)
        assertTrue(finishAFlipped.compareTo(finishBFlipped) < 0)
        assertTrue(finishBFlipped.compareTo(finishA)        > 0)
        assertTrue(finishBFlipped.compareTo(finishAFlipped) > 0)
    }

    @Test
    void testFlip() {
        def normal = new Interval(80, 90)
        def flipped = normal.flip()

        // flipping should switch from/to ..
        assertTrue(normal.from == flipped.to)
        assertTrue(normal.to == flipped.from)

        // .. but keep min/max the same
        assertTrue(normal.lowEnd == flipped.lowEnd)
        assertTrue(normal.highEnd == flipped.highEnd)
    }

    @Test
    void testAsAscending() {
        assertEquals("ascending interval.asAscending() shouldn't change the interval", new Interval(10, 20), new Interval(10, 20).asAscending())
        assertEquals("descending interval.asAscending() should flip", positiveAscending, positiveDescending.asAscending())
    }

    @Test
    void testLength() {
        // single-length points
        assertEquals(1, positivePoint.length())
        assertEquals(1, negativePoint.length())

        // Length Start And End Next To Each Other
        def shortInterval = new Interval(4L, 5L)
        assertEquals(2, shortInterval.length())
        assertEquals(2, shortInterval.flip().length())

        // zero-spanning
        assertEquals(21, zeroSpanningAscending.length())
        assertEquals(21, zeroSpanningDescending.length())

        // Length Very Big
        long almostLongMaxValue = Long.MAX_VALUE - 1 // -1, to make (inclusive) length later equal to long.max
        def hugeInterval = new Interval(0L, almostLongMaxValue)
        assertEquals(Long.MAX_VALUE, hugeInterval.length())
        assertEquals(Long.MAX_VALUE, hugeInterval.flip().length())

        // negative intervals
        assertEquals(11L, negativeAscending.length())
        assertEquals(11L, negativeDescending.length())

        long almostLongMinValue = Long.MIN_VALUE + 1 // +1, to make (inclusive) length later equal to abs(long.min)
        def hugeNegative = new Interval(0L, almostLongMinValue)
        assertEquals(+Long.MIN_VALUE, hugeNegative.length())
        assertEquals(+Long.MIN_VALUE, hugeNegative.flip().length())
    }

    @Test
    void testEquals() {
        assertEquals("Interval should be equal to itself", equalA, equalA)
        assertEquals("Interval should be equal to same-range different instance", equalA, equalB)

        // with different instance
        assertEquals("positive point-interval should be equal regardless of instance", new Interval(10, 10), new Interval(10, 10))
        assertEquals("negative point-interval should be equal regardless of instance", new Interval(-10, -10), new Interval(-10, -10))
        assertEquals("zero-spanning intervals should be equal regardless of instance", new Interval(-10, 10), new Interval(-10, 10))

        assertNotEquals("overlapping interval shouldn't be equal", new Interval(10, 11), new Interval(11, 12))
        assertNotEquals("zero-spanning, containing interval shouldn't be equal", new Interval(-10, 11), new Interval(-11, 12))
        assertNotEquals("direction should matter, shouldn't be equal", new Interval(10, 20), new Interval(20, 10))
    }

    /**
     * equal
     * <pre>
     * A ===
     * B ===
     * </pre>
     */
    @Test
    void testIntersectEqual() {
        assertEquals(equalA, equalA.intersect(equalA))
        assertEquals(equalA, equalA.intersect(equalB))

        assertEquals(equalA, equalA.intersect(equalB))
        assertEquals(equalA, equalA.intersect(equalB.flip()))
        assertEquals(equalA.flip(), equalA.flip().intersect(equalB))
        assertEquals(equalA.flip(), equalA.flip().intersect(equalB.flip()))
    }

    /**
     * before
     * <pre>
     * A ===
     * B      ===
     * </pre>
     */
    @Test
    void testIntersectBefore() {
        assertNull(beforeA.intersect(beforeB))
        assertNull(beforeA.intersect(beforeBFlipped))
        assertNull(beforeB.intersect(beforeA))
        assertNull(beforeB.intersect(beforeAFlipped))
        assertNull(beforeAFlipped.intersect(beforeB))
        assertNull(beforeAFlipped.intersect(beforeBFlipped))
        assertNull(beforeBFlipped.intersect(beforeA))
        assertNull(beforeBFlipped.intersect(beforeAFlipped))
    }

    /**
     * meeting
     * <pre>
     * A ===
     * B    ===
     * </pre>
     */
    @Test
    void testIntersectMeeting() {
        assertNull(meetA.intersect(meetB))
        assertNull(meetA.intersect(meetBFlipped))
        assertNull(meetB.intersect(meetA))
        assertNull(meetB.intersect(meetAFlipped))
        assertNull(meetAFlipped.intersect(meetB))
        assertNull(meetAFlipped.intersect(meetBFlipped))
        assertNull(meetBFlipped.intersect(meetA))
        assertNull(meetBFlipped.intersect(meetAFlipped))
    }

    /**
     * overlapping
     * <pre>
     * A ====
     * B    ====
     * </pre>
     */
    @Test
    void testIntersectOverlapping() {
        def expected = new Interval(20, 30)
        def expectedFlipped = expected.flip()
        assertEquals(expected, overlapA.intersect(overlapB))
        assertEquals(expected, overlapA.intersect(overlapBFlipped))
        assertEquals(expected, overlapB.intersect(overlapA))
        assertEquals(expected, overlapB.intersect(overlapAFlipped))
        assertEquals(expectedFlipped, overlapAFlipped.intersect(overlapB))
        assertEquals(expectedFlipped, overlapAFlipped.intersect(overlapBFlipped))
        assertEquals(expectedFlipped, overlapBFlipped.intersect(overlapA))
        assertEquals(expectedFlipped, overlapBFlipped.intersect(overlapAFlipped))
    }

    /**
     * one contains other
     * <pre>
     * A =======
     * B   ===
     * </pre>
     */
    @Test
    void testIntersectContaining() {
        assertEquals(spanB, spanA.intersect(spanB))
        assertEquals(spanB, spanA.intersect(spanBFlipped))
        assertEquals(spanB, spanB.intersect(spanA))
        assertEquals(spanB, spanB.intersect(spanAFlipped))
        assertEquals(spanBFlipped, spanAFlipped.intersect(spanB))
        assertEquals(spanBFlipped, spanAFlipped.intersect(spanBFlipped))
        assertEquals(spanBFlipped, spanBFlipped.intersect(spanA))
        assertEquals(spanBFlipped, spanBFlipped.intersect(spanAFlipped))
    }

    /**
     * start same
     * <pre>
     * A ======
     * B ===
     * </pre>
     */
    @Test
    void testIntersectStartSame() {
        assertEquals(startB, startA.intersect(startB))
        assertEquals(startB, startA.intersect(startBFlipped))
        assertEquals(startB, startB.intersect(startA))
        assertEquals(startB, startB.intersect(startAFlipped))
        assertEquals(startBFlipped, startAFlipped.intersect(startB))
        assertEquals(startBFlipped, startAFlipped.intersect(startBFlipped))
        assertEquals(startBFlipped, startBFlipped.intersect(startA))
        assertEquals(startBFlipped, startBFlipped.intersect(startAFlipped))
    }

    /**
     * finish same
     * <pre>
     * A ======
     * B    ===
     * </pre>
     */
    @Test
    void testIntersectFinishSame() {
        assertEquals(finishB, finishA.intersect(finishB))
        assertEquals(finishB, finishA.intersect(finishBFlipped))
        assertEquals(finishB, finishB.intersect(finishA))
        assertEquals(finishB, finishB.intersect(finishAFlipped))
        assertEquals(finishBFlipped, finishAFlipped.intersect(finishB))
        assertEquals(finishBFlipped, finishAFlipped.intersect(finishBFlipped))
        assertEquals(finishBFlipped, finishBFlipped.intersect(finishA))
        assertEquals(finishBFlipped, finishBFlipped.intersect(finishAFlipped))
    }

    /**
     * equal
     * <pre>
     * A ===
     * B ===
     * </pre>
     */
    @Test
    void testUnionEqual() {
        assertEquals(equalA, equalA.union(equalA))
        assertEquals(equalA, equalA.union(equalB))

        assertEquals(equalA, equalA.union(equalB))
        assertEquals(equalA, equalA.union(equalB.flip()))
        assertEquals(equalA.flip(), equalA.flip().union(equalB))
        assertEquals(equalA.flip(), equalA.flip().union(equalB.flip()))
    }

    /**
     * before
     * <pre>
     * A ===
     * B      ===
     * </pre>
     */
    @Test
    void testUnionBefore() {
        assertNull(beforeA.union(beforeB))
        assertNull(beforeA.union(beforeBFlipped))
        assertNull(beforeB.union(beforeA))
        assertNull(beforeB.union(beforeAFlipped))
        assertNull(beforeAFlipped.union(beforeB))
        assertNull(beforeAFlipped.union(beforeBFlipped))
        assertNull(beforeBFlipped.union(beforeA))
        assertNull(beforeBFlipped.union(beforeAFlipped))
    }

    /**
     * meeting
     * <pre>
     * A ===
     * B    ===
     * </pre>
     */
    @Test
    void testUnionMeeting() {
        def expected = new Interval(10, 40)
        def expectedFlipped = expected.flip()
        assertEquals(expected, meetA.union(meetB))
        assertEquals(expected, meetA.union(meetBFlipped))
        assertEquals(expected, meetB.union(meetA))
        assertEquals(expected, meetB.union(meetAFlipped))
        assertEquals(expectedFlipped, meetAFlipped.union(meetB))
        assertEquals(expectedFlipped, meetAFlipped.union(meetBFlipped))
        assertEquals(expectedFlipped, meetBFlipped.union(meetA))
        assertEquals(expectedFlipped, meetBFlipped.union(meetAFlipped))
    }

    /**
     * overlapping
     * <pre>
     * A ====
     * B    ====
     * </pre>
     */
    @Test
    void testUnionOverlapping() {
        def expected = new Interval(0, 40)
        def expectedFlipped = expected.flip()
        assertEquals(expected, overlapA.union(overlapB))
        assertEquals(expected, overlapA.union(overlapBFlipped))
        assertEquals(expected, overlapB.union(overlapA))
        assertEquals(expected, overlapB.union(overlapAFlipped))
        assertEquals(expectedFlipped, overlapAFlipped.union(overlapB))
        assertEquals(expectedFlipped, overlapAFlipped.union(overlapBFlipped))
        assertEquals(expectedFlipped, overlapBFlipped.union(overlapA))
        assertEquals(expectedFlipped, overlapBFlipped.union(overlapAFlipped))
    }

    /**
     * one contains other
     * <pre>
     * A =======
     * B   ===
     * </pre>
     */
    @Test
    void testUnionContaining() {
        assertEquals(spanA, spanA.union(spanB))
        assertEquals(spanA, spanA.union(spanBFlipped))
        assertEquals(spanA, spanB.union(spanA))
        assertEquals(spanA, spanB.union(spanAFlipped))
        assertEquals(spanAFlipped, spanAFlipped.union(spanB))
        assertEquals(spanAFlipped, spanAFlipped.union(spanBFlipped))
        assertEquals(spanAFlipped, spanBFlipped.union(spanA))
        assertEquals(spanAFlipped, spanBFlipped.union(spanAFlipped))
    }

    /**
     * start same
     * <pre>
     * A ======
     * B ===
     * </pre>
     */
    @Test
    void testUnionStartSame() {
        assertEquals(startA, startA.union(startB))
        assertEquals(startA, startA.union(startBFlipped))
        assertEquals(startA, startB.union(startA))
        assertEquals(startA, startB.union(startAFlipped))
        assertEquals(startAFlipped, startAFlipped.union(startB))
        assertEquals(startAFlipped, startAFlipped.union(startBFlipped))
        assertEquals(startAFlipped, startBFlipped.union(startA))
        assertEquals(startAFlipped, startBFlipped.union(startAFlipped))
    }

    /**
     * finish same
     * <pre>
     * A ======
     * B    ===
     * </pre>
     */
    @Test
    void testUnionFinishSame() {
        assertEquals(finishA, finishA.union(finishB))
        assertEquals(finishA, finishA.union(finishBFlipped))
        assertEquals(finishA, finishB.union(finishA))
        assertEquals(finishA, finishB.union(finishAFlipped))
        assertEquals(finishAFlipped, finishAFlipped.union(finishB))
        assertEquals(finishAFlipped, finishAFlipped.union(finishBFlipped))
        assertEquals(finishAFlipped, finishBFlipped.union(finishA))
        assertEquals(finishAFlipped, finishBFlipped.union(finishAFlipped))
    }

    @Test
    void testAbuts() {
        // 1 equal
        assertFalse(equalA.abuts(equalA))
        assertFalse(equalA.abuts(equalB))
        assertFalse(equalB.abuts(equalA.flip()))
        assertFalse(equalB.abuts(equalB.flip()))
        assertFalse(equalA.flip().abuts(equalA))
        assertFalse(equalA.flip().abuts(equalB))
        assertFalse(equalB.flip().abuts(equalA.flip()))
        assertFalse(equalB.flip().abuts(equalB.flip()))

        // 2 before
        assertFalse(beforeA.abuts(beforeB))
        assertFalse(beforeA.abuts(beforeBFlipped))
        assertFalse(beforeB.abuts(beforeA))
        assertFalse(beforeB.abuts(beforeAFlipped))
        assertFalse(beforeAFlipped.abuts(beforeB))
        assertFalse(beforeAFlipped.abuts(beforeBFlipped))
        assertFalse(beforeBFlipped.abuts(beforeA))
        assertFalse(beforeBFlipped.abuts(beforeAFlipped))


        // 3 meet
        assertTrue(meetA.abuts(meetB))
        assertTrue(meetA.abuts(meetBFlipped))
        assertTrue(meetB.abuts(meetA))
        assertTrue(meetB.abuts(meetAFlipped))
        assertTrue(meetAFlipped.abuts(meetB))
        assertTrue(meetAFlipped.abuts(meetBFlipped))
        assertTrue(meetBFlipped.abuts(meetA))
        assertTrue(meetBFlipped.abuts(meetAFlipped))

        // 4 span / during
        assertFalse(spanA.abuts(spanB))
        assertFalse(spanA.abuts(spanBFlipped))
        assertFalse(spanB.abuts(spanA))
        assertFalse(spanB.abuts(spanAFlipped))
        assertFalse(spanAFlipped.abuts(spanB))
        assertFalse(spanAFlipped.abuts(spanBFlipped))
        assertFalse(spanBFlipped.abuts(spanA))
        assertFalse(spanBFlipped.abuts(spanAFlipped))

        // 5 overlap
        assertFalse(overlapA.abuts(overlapB))
        assertFalse(overlapA.abuts(overlapBFlipped))
        assertFalse(overlapB.abuts(overlapA))
        assertFalse(overlapB.abuts(overlapAFlipped))
        assertFalse(overlapAFlipped.abuts(overlapB))
        assertFalse(overlapAFlipped.abuts(overlapBFlipped))
        assertFalse(overlapBFlipped.abuts(overlapA))
        assertFalse(overlapBFlipped.abuts(overlapAFlipped))

        // 6 start
        assertFalse(startA.abuts(startB))
        assertFalse(startA.abuts(startBFlipped))
        assertFalse(startB.abuts(startA))
        assertFalse(startB.abuts(startAFlipped))
        assertFalse(startAFlipped.abuts(startB))
        assertFalse(startAFlipped.abuts(startBFlipped))
        assertFalse(startBFlipped.abuts(startA))
        assertFalse(startBFlipped.abuts(startAFlipped))

        // 7 finish
        assertFalse(finishA.abuts(finishB))
        assertFalse(finishA.abuts(finishBFlipped))
        assertFalse(finishB.abuts(finishA))
        assertFalse(finishB.abuts(finishAFlipped))
        assertFalse(finishAFlipped.abuts(finishB))
        assertFalse(finishAFlipped.abuts(finishBFlipped))
        assertFalse(finishBFlipped.abuts(finishA))
        assertFalse(finishBFlipped.abuts(finishAFlipped))
    }

    @Test
    void testOverlaps() {
        // 1 equal
        assertTrue(equalA.overlaps(equalA))

        assertTrue(equalA.overlaps(equalB))
        assertTrue(equalA.overlaps(equalB.flip()))
        assertTrue(equalB.overlaps(equalA))
        assertTrue(equalB.overlaps(equalA.flip()))
        assertTrue(equalA.flip().overlaps(equalB))
        assertTrue(equalA.flip().overlaps(equalB.flip()))
        assertTrue(equalB.flip().overlaps(equalA))
        assertTrue(equalB.flip().overlaps(equalA.flip()))

        // 2 before
        assertFalse(beforeA.overlaps(beforeB))
        assertFalse(beforeA.overlaps(beforeBFlipped))
        assertFalse(beforeB.overlaps(beforeA))
        assertFalse(beforeB.overlaps(beforeAFlipped))
        assertFalse(beforeAFlipped.overlaps(beforeB))
        assertFalse(beforeAFlipped.overlaps(beforeBFlipped))
        assertFalse(beforeBFlipped.overlaps(beforeA))
        assertFalse(beforeBFlipped.overlaps(beforeAFlipped))

        // 3 meet
        assertFalse(meetA.overlaps(meetB))
        assertFalse(meetA.overlaps(meetBFlipped))
        assertFalse(meetB.overlaps(meetA))
        assertFalse(meetB.overlaps(meetAFlipped))
        assertFalse(meetAFlipped.overlaps(meetB))
        assertFalse(meetAFlipped.overlaps(meetBFlipped))
        assertFalse(meetBFlipped.overlaps(meetA))
        assertFalse(meetBFlipped.overlaps(meetAFlipped))

        // 4 overlap
        assertTrue(overlapA.overlaps(overlapB))
        assertTrue(overlapA.overlaps(overlapBFlipped))
        assertTrue(overlapB.overlaps(overlapA))
        assertTrue(overlapB.overlaps(overlapAFlipped))
        assertTrue(overlapAFlipped.overlaps(overlapB))
        assertTrue(overlapAFlipped.overlaps(overlapBFlipped))
        assertTrue(overlapBFlipped.overlaps(overlapA))
        assertTrue(overlapBFlipped.overlaps(overlapAFlipped))

        // 5 span
        assertTrue(spanA.overlaps(spanB))
        assertTrue(spanA.overlaps(spanBFlipped))
        assertTrue(spanB.overlaps(spanA))
        assertTrue(spanB.overlaps(spanAFlipped))
        assertTrue(spanAFlipped.overlaps(spanB))
        assertTrue(spanAFlipped.overlaps(spanBFlipped))
        assertTrue(spanBFlipped.overlaps(spanA))
        assertTrue(spanBFlipped.overlaps(spanAFlipped))

        // 6 start
        assertTrue(startA.overlaps(startB))
        assertTrue(startA.overlaps(startBFlipped))
        assertTrue(startB.overlaps(startA))
        assertTrue(startB.overlaps(startAFlipped))
        assertTrue(startAFlipped.overlaps(startB))
        assertTrue(startAFlipped.overlaps(startBFlipped))
        assertTrue(startBFlipped.overlaps(startA))
        assertTrue(startBFlipped.overlaps(startAFlipped))

        // 7 finish
        assertTrue(finishA.overlaps(finishB))
        assertTrue(finishA.overlaps(finishBFlipped))
        assertTrue(finishB.overlaps(finishA))
        assertTrue(finishB.overlaps(finishAFlipped))
        assertTrue(finishAFlipped.overlaps(finishB))
        assertTrue(finishAFlipped.overlaps(finishBFlipped))
        assertTrue(finishBFlipped.overlaps(finishA))
        assertTrue(finishBFlipped.overlaps(finishAFlipped))
    }

    @Test
    void testAscending() {
        assertTrue("point-intervals should be ascending", positivePoint.isAscending())

        assertTrue("positive ascending intervals should be ascending", positiveAscending.isAscending())
        assertTrue("negative ascending intervals should be ascending", negativeAscending.isAscending())
        assertTrue("zero-spanning ascending intervals should be ascending", zeroSpanningAscending.isAscending())

        assertFalse("positive descending intervals shouldn't be ascending", positiveDescending.isAscending())
        assertFalse("negative descending intervals shouldn't be ascending", negativeDescending.isAscending())
        assertFalse("zero-spanning descending intervals shouldn't be ascending", zeroSpanningDescending.isAscending())
    }
}
