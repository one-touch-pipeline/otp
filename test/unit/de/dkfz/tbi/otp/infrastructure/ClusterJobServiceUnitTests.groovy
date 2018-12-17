package de.dkfz.tbi.otp.infrastructure


import org.joda.time.*
import org.junit.*

class ClusterJobServiceUnitTests extends GroovyTestCase {

    static final LocalDate SDATE_LOCALDATE = new LocalDate()
    static final LocalDate EDATE_LOCALDATE = SDATE_LOCALDATE.plusDays(1)
    static final DateTime SDATE_DATETIME = SDATE_LOCALDATE.toDateTimeAtStartOfDay()
    static final DateTime EDATE_DATETIME = EDATE_LOCALDATE.toDateTimeAtStartOfDay()


    ClusterJobService clusterJobService = new ClusterJobService()

    @Test
    void test_getDaysAndHoursBetween_WhenFromEqualsTo_ShouldReturnListOfTwentyFiveDates() {
        List dates = (0..24).collect { SDATE_LOCALDATE.toDateTimeAtStartOfDay().plusHours(it)}

        assert dates == clusterJobService.getDaysAndHoursBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)
    }

    @Test
    void test_getLabels_WhenMaxEqualsThousandAndQuotEqualsTen_ShouldReturnMapWithStringListAndDoubleListEachContainingTenValuesEachValueAQuotHigherThanThePreviousValue() {
        List labels = clusterJobService.getLabels(1000, 10)

        assert (1..10).collect { (it * 100.0) as String } == labels.first()
        assert (1..10).collect { (it * 100.0) as Double } == labels.last()
    }
}
