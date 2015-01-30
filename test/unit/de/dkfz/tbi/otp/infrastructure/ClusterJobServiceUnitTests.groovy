package de.dkfz.tbi.otp.infrastructure

import org.apache.tools.ant.types.resources.selectors.InstanceOf
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNotNull

class ClusterJobServiceUnitTests extends GroovyTestCase {

    public static final LocalDate SDATE_LOCALDATE = new LocalDate()
    public static final LocalDate EDATE_LOCALDATE = SDATE_LOCALDATE.plusDays(1)
    public static final DateTime SDATE_DATETIME = SDATE_LOCALDATE.toDateTimeAtStartOfDay()
    public static final DateTime EDATE_DATETIME = EDATE_LOCALDATE.toDateTimeAtStartOfDay()


    ClusterJobService clusterJobService = new ClusterJobService()

    @Test
    void test_getDaysAndHoursBetween_WhenFromEqualsTo_ShouldReturnListOfTwentyFiveDatesAsStrings() {
        List dates = (0..24).collect { SDATE_LOCALDATE.toDateTimeAtStartOfDay().plusHours(it).toString('yyyy-MM-dd HH:mm:ss') }

        assert dates == clusterJobService.getDaysAndHoursBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)
    }

    @Test
    void test_getLabels_WhenMaxEqualsThousandAndQuotEqualsTen_ShouldReturnMapWithStringListAndDoubleListEachContainingTenValuesEachValueAQuotHigherThanThePreviousValue() {
        List labels = clusterJobService.getLabels(1000, 10)

        assert (1..10).collect { (it * 100.0) as String } == labels.first()
        assert (1..10).collect { (it * 100.0) as Double } == labels.last()
    }
}
