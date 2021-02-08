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
package de.dkfz.tbi.otp.tracking

import spock.lang.Specification

class ProcessingTimeStatisticsServiceSpec extends Specification {

    void "getFormattedPeriod, when d1 is missing, return empty String"() {
        expect:
        "" == ProcessingTimeStatisticsService.getFormattedPeriod(null, new Date())
    }

    void "getFormattedPeriod, when d2 is missing, return empty String"() {
        expect:
        "" == ProcessingTimeStatisticsService.getFormattedPeriod(new Date(), null)
    }

    void "getFormattedPeriod, when period is negative, return formatted String marked negativ "() {
        given:
        int dateDiff = 1
        Date date = new Date()

        expect:
        "-${dateDiff.toString().padLeft(2, '0')}d 00h 00m" == ProcessingTimeStatisticsService.getFormattedPeriod(date, date - dateDiff)
    }

    void "getFormattedPeriod, when period is positive, return formatted String"() {
        given:
        int dateDiff = 1
        Date date = new Date()

        expect:
        "${dateDiff.toString().padLeft(2, '0')}d 00h 00m" == ProcessingTimeStatisticsService.getFormattedPeriod(date, date + dateDiff)
    }

    void "getFormattedPeriod, when period is longer than a week, return formatted String with days as most superior unit"() {
        given:
        int dateDiff = 14
        Date date = new Date()

        expect:
        "-${dateDiff.toString().padLeft(2, '0')}d 00h 00m" == ProcessingTimeStatisticsService.getFormattedPeriod(date, date - dateDiff)
    }
}
