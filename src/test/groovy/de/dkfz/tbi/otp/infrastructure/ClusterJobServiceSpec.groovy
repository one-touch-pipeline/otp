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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.HelperUtils

import java.time.LocalDate
import java.time.ZoneId

class ClusterJobServiceSpec extends Specification implements DataTest, ServiceUnitTest<ClusterJobService> {

    static final LocalDate START_DATE = LocalDate.now()
    static final LocalDate END_DATE = START_DATE.plusDays(1)

    ClusterJobService clusterJobService = new ClusterJobService()

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                JobExecutionPlan,
                ProcessingStep,
                ClusterJob,
        ]
    }

    void test_DateTimeIntervalWithHourBuckets_hourBuckets_WhenInputIs24_hours_ThenShouldReturnListOfTwentyFiveDates() {
        given:
        List dates = (0..48).collect {
            START_DATE.atStartOfDay(ZoneId.systemDefault()).plusHours(it)
        }

        expect:
        dates == new ClusterJobService.DateTimeIntervalWithHourBuckets(START_DATE, END_DATE).hourBuckets
    }

    void test_getLabels_WhenMaxEqualsThousandAndQuotEqualsTen_ShouldReturnMapWithStringListAndDoubleListEachContainingTenValuesEachValueAQuotHigherThanThePreviousValue() {
        given:
        List labels = clusterJobService.getLabels(1000, 10)

        expect:
        (1..10).collect { (it * 100.0) as String } == labels.first()
        (1..10).collect { (it * 100.0) as Double } == labels.last()
    }

    void test_getClusterJobByIdentifier() {
        given:
        ClusterJob clusterJob = DomainFactory.createClusterJob()
        ClusterJobIdentifier identifier = new ClusterJobIdentifier(clusterJob.clusterJobId)
        DomainFactory.createClusterJob(
                clusterJobId: HelperUtils.uniqueString,
        )
        DomainFactory.createClusterJob(
                clusterJobId: identifier.clusterJobId,
        )

        expect:
        clusterJobService.getClusterJobByIdentifier(identifier, clusterJob.processingStep) == clusterJob
    }
}
