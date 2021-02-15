/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.infrastructure

import grails.test.mixin.Mock
import org.joda.time.LocalDate
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.Realm

@Mock([
        Realm, //It is necessary to mock a class to have the needed configured GORM
])
class ClusterJobServiceUnitTests {

    static final LocalDate SDATE_LOCALDATE = new LocalDate()
    static final LocalDate EDATE_LOCALDATE = SDATE_LOCALDATE.plusDays(1)

    ClusterJobService clusterJobService = new ClusterJobService()

    @Test
    void test_DateTimeIntervalWithHourBuckets_hourBuckets_WhenInputIs24_hours_ThenShouldReturnListOfTwentyFiveDates() {
        List dates = (0..24).collect { SDATE_LOCALDATE.toDateTimeAtStartOfDay().plusHours(it) }

        assert dates == new ClusterJobService.DateTimeIntervalWithHourBuckets(SDATE_LOCALDATE, SDATE_LOCALDATE).hourBuckets
    }

    @Test
    void test_getLabels_WhenMaxEqualsThousandAndQuotEqualsTen_ShouldReturnMapWithStringListAndDoubleListEachContainingTenValuesEachValueAQuotHigherThanThePreviousValue() {
        List labels = clusterJobService.getLabels(1000, 10)

        assert (1..10).collect { (it * 100.0) as String } == labels.first()
        assert (1..10).collect { (it * 100.0) as Double } == labels.last()
    }
}
