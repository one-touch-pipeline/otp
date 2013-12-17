package de.dkfz.tbi.ngstools.bedUtils

import static de.dkfz.tbi.ngstools.bedUtils.IntervalFixtures.*
import static org.junit.Assert.*

import org.junit.*

/**
 */
class MergingIntervalCollectionTest {
    MergingIntervalCollection tree

    @Before
    void before() {
        tree = new MergingIntervalCollection()
    }

    @Test
    void testAdd() {
        assertEquals(0, tree.values.size())

        // test single addition
        tree.add(new Interval(10, 20))
        assertTrue(tree.values.contains(new Interval(10, 20)))

        // test non-overlapping addition
        tree.add(new Interval(30, 40))
        assertEquals(2, tree.values.size())
        assertTrue(tree.values.contains(new Interval(10, 20)))
        assertTrue(tree.values.contains(new Interval(30, 40)))

        // test overlapping interval addition
        tree.add(new Interval(15, 25))
        assertEquals(2, tree.values.size())
        assertTrue(tree.values.contains(new Interval(10, 25)))
        assertTrue(tree.values.contains(new Interval(30, 40)))
        tree.add(new Interval(21, 31)) // should merge all existing items into one
        assertEquals(1, tree.values.size())
        assertTrue(tree.values.contains(new Interval(10, 40)))

        // test abutting intervals
        tree.add(5, 9)
        assertEquals(1, tree.values.size())
        assertTrue("adding abutting intervals should also merge", tree.values.contains(new Interval(5, 40)))
        tree.add(4, 1)
        assertEquals(1, tree.values.size())
        assertTrue("adding flipped abutting intervals should also merge", tree.values.contains(new Interval(1, 40)) || tree.values.contains(new Interval(40, 1)))

        // clear playing field
        tree.clear()

        // test a flipped interval
        tree.add(new Interval(1, 40))
        tree.add(new Interval(50, 45))
        assertEquals(2, tree.values.size())
        assertTrue(tree.values.contains(new Interval(1, 40)))
        assertTrue(tree.values.contains(new Interval(50, 45)))
        // test merging flipped intervals, in these cases, resulting ordering is not defined.
        tree.add(new Interval(46, 39))
        assertEquals(1, tree.values.size())
        assertTrue("adding flipped intervals should also merge overlapping", tree.values.contains(new Interval(1, 50)) || tree.values.contains(new Interval(50, 1)))

        // clear playing field
        tree.clear()

        // test spanning intervals
        tree.add(30, 50)
        tree.add(1, 100)
        assertEquals(1, tree.values.size())
        assertTrue(tree.values.contains(new Interval(1, 100)))
        tree.add(60, 70)
        assertEquals(1, tree.values.size())
        assertTrue(tree.values.contains(new Interval(1, 100)))
    }

    @Test
    void testOverlapping() {
        // set up two intervals to play with.
        tree.add(new Interval(10,20))
        tree.add(new Interval(30,40))

        // interval at end, normal and flipped
        def overlaps1 = tree.getOverlappingIntervals(15, 25)
        assertTrue(overlaps1.contains(new Interval(10,20)))
        assertEquals(1, overlaps1.size())
        def overlaps1flip = tree.getOverlappingIntervals(25, 15)
        assertTrue(overlaps1flip.contains(new Interval(10,20)))
        assertEquals(1, overlaps1flip.size())


        // interval at beginning, normal and flipped
        def overlaps2 = tree.getOverlappingIntervals(25, 35)
        assertTrue(overlaps2.contains(new Interval(30,40)))
        assertEquals(1, overlaps2.size())
        def overlaps2flip = tree.getOverlappingIntervals(35, 25)
        assertTrue(overlaps2flip.contains(new Interval(30,40)))
        assertEquals(1, overlaps2flip.size())

        // multi-overlap, normal and flipped
        def overlaps3 = tree.getOverlappingIntervals(15, 35)
        assertTrue(overlaps3.contains(new Interval(10, 20)))
        assertTrue(overlaps3.contains(new Interval(30, 40)))
        assertEquals(2, overlaps3.size())
        def overlaps3flip = tree.getOverlappingIntervals(35, 15)
        assertTrue(overlaps3flip.contains(new Interval(10, 20)))
        assertTrue(overlaps3flip.contains(new Interval(30, 40)))
        assertEquals(2, overlaps3flip.size())

        // containing interval, normal and flipped
        def overlaps4 = tree.getOverlappingIntervals(0, 100)
        assertTrue(overlaps4.contains(new Interval(10, 20)))
        assertTrue(overlaps4.contains(new Interval(30, 40)))
        assertEquals(2, overlaps4.size())
        def overlaps4flipped = tree.getOverlappingIntervals(100, 0)
        assertTrue(overlaps4flipped.contains(new Interval(10, 20)))
        assertTrue(overlaps4flipped.contains(new Interval(30, 40)))
        assertEquals(2, overlaps4flipped.size())
    }

    /*
     * Tests the different Allen-combinations.
     *
     * (see {@link IntervalFixtures} for reference)
     */

    /** 1 equal */
    @Test
    void testOverlappingEqual() {
        tree.add(equalA)

        def overlaps = tree.getOverlappingIntervals(equalA)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(equalA))
        assertTrue(overlaps.contains(equalB))
    }

    /** 2 before */
    @Test
    void testOverlappingBefore() {
        tree.add(beforeA)
        assertEquals(0, tree.getOverlappingIntervals(beforeB).size())
    }

    /** 3 meet */
    @Test
    void testOverlappingMeet() {
        tree.add(meetA)
        assertEquals(0, tree.getOverlappingIntervals(meetB).size())
    }

    /** 4 overlap, 1/2 */
    @Test
    void testOverlappingOverlap1() {
        tree.add(overlapA)
        def overlaps = tree.getOverlappingIntervals(overlapB)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(overlapA))
    }

    /** 4 overlap, 2/2 */
    @Test
    void testOverlappingOverlap2() {
        tree.add(overlapB)
        def overlaps = tree.getOverlappingIntervals(overlapA)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(overlapB))
    }

    /** 5 span, 1/2 */
    @Test
    void testOverlappingSpan1() {
        tree.add(spanA)
        def overlaps = tree.getOverlappingIntervals(spanB)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(spanA))
    }

    /** 5 span, 2/2 */
    @Test
    void testOverlappingSpan2() {
        tree.add(spanB)
        def overlaps = tree.getOverlappingIntervals(spanA)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(spanB))
    }

    /** 6 start, 1/2 */
    @Test
    void testOverlappingStart1() {
        tree.add(startA)
        def overlaps = tree.getOverlappingIntervals(startB)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(startA))
    }

    /** 6 start, 2/2 */
    @Test
    void testOverlappingStart2() {
        tree.add(startB)
        def overlaps = tree.getOverlappingIntervals(startA)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(startB))
    }

    /** 7 finish, 1/2 */
    @Test
    void testOverlappingFinish1() {
        tree.add(finishA)
        def overlaps = tree.getOverlappingIntervals(finishB)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(finishA))
    }

        /** 7 finish, 2/2 */
    @Test
    void testOverlappingFinish2() {
        tree.add(finishB)
        def overlaps = tree.getOverlappingIntervals(finishA)
        assertEquals(1, overlaps.size())
        assertTrue(overlaps.contains(finishB))
    }

    @Test
    void testClear() {
        tree.add(overlapA)

        tree.clear()

        assertTrue(tree.values.empty)
        assertTrue(tree.getOverlappingIntervals(overlapA).empty)
        assertTrue(tree.getOverlappingIntervals(overlapB).empty)
    }
}
