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

package de.dkfz.tbi.otp.ngsdata

import grails.test.spock.IntegrationSpec
import spock.lang.Unroll


class SampleServiceIntegrationSpec extends IntegrationSpec {
    SampleService sampleService

    @Unroll
    void "test getCountOfSamplesForSpecifiedPeriodAndProjects for given date"() {
        given:
        Date baseDate = new Date(0, 0, 10)
        Date startDate = startDateOffset  == null ? null : baseDate.minus(startDateOffset)
        Date endDate = endDateOffset == null ? null : baseDate.minus(endDateOffset)

        DataFile dataFile = DomainFactory.createDataFile()
        dataFile.dateCreated = baseDate.minus(1)

        when:
        int samples = sampleService.getCountOfSamplesForSpecifiedPeriodAndProjects(startDate, endDate, [dataFile.project])

        then:
        samples == expectedSamples

        where:
        startDateOffset | endDateOffset || expectedSamples
        2               | 0             || 1
        8               | 2             || 0
        null            | null          || 1
    }
}
