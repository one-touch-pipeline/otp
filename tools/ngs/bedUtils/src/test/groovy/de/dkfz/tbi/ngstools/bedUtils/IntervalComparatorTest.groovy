package de.dkfz.tbi.ngstools.bedUtils

import static org.junit.Assert.*
import org.junit.*
import edu.stanford.nlp.util.*

/**
 * Tests for the compare() method
 *
 * 1. start of interval A before start of interval B; exp.: A, B
 * 2. start of interval B before start of interval A; exp.: B, A
 * 3. start of interval A and B same, end of interval B > end of interval A; exp.: A, B
 * 4. start of interval A and B same, end of interval A > end of interval B; exp.: B, A
 * 5. start and end of interval A and B are exactly the same; exp.: A, B
 *
 * order in orginal list is always A, B
 *
 */
class IntervalComparatorTest {

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
     * a ---
     * b   ---
     */
    @Test
    void compareTest1() {
        Interval intervalA = new Interval(10, 20, 0)
        intervalList.add(intervalA)
        Interval intervalB = new Interval(15, 25, 0)
        intervalList.add(intervalB)
        Collections.sort(intervalList, IntervalComparator.INSTANCE)
        // expected order: A, B
        assertEquals(intervalA, intervalList.get(0))
        assertEquals(intervalB, intervalList.get(1))
    }

    /*
     * a   ---
     * b ---
     */
    @Test
    void compareTest2() {
        Interval intervalA = new Interval(15, 25, 0)
        intervalList.add(intervalA)
        Interval intervalB = new Interval(10, 20, 0)
        intervalList.add(intervalB)
        Collections.sort(intervalList, IntervalComparator.INSTANCE)
        // expected order: B, A
        assertEquals(intervalB, intervalList.get(0))
        assertEquals(intervalA, intervalList.get(1))
    }

    /*
     * a ---
     * b -----
     */
    @Test
    void compareTest3() {
        Interval intervalA = new Interval(10, 20, 0)
        intervalList.add(intervalA)
        Interval intervalB = new Interval(10, 30, 0)
        intervalList.add(intervalB)
        Collections.sort(intervalList, IntervalComparator.INSTANCE)
        // expected order: A, B
        assertEquals(intervalA, intervalList.get(0))
        assertEquals(intervalB, intervalList.get(1))
    }

    /*
     * a -----
     * b ---
     */
    @Test
    void compareTest4() {
        Interval intervalA = new Interval(10, 30, 0)
        intervalList.add(intervalA)
        Interval intervalB = new Interval(10, 20, 0)
        intervalList.add(intervalB)
        Collections.sort(intervalList, IntervalComparator.INSTANCE)
        // expected order: B, A
        assertEquals(intervalB, intervalList.get(0))
        assertEquals(intervalA, intervalList.get(1))
    }

    /*
     * a ---
     * b ---
     */
    @Test
    void compareTest5() {
        Interval intervalA = new Interval(10, 20, 0)
        intervalList.add(intervalA)
        Interval intervalB = new Interval(10, 20, 0)
        intervalList.add(intervalB)
        Collections.sort(intervalList, IntervalComparator.INSTANCE)
        // expected order: A, B
        assertTrue(intervalA.is(intervalList.get(0)))
        assertTrue(intervalB.is(intervalList.get(1)))
    }
}
