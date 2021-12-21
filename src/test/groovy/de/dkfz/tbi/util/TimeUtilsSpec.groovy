/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.util

import spock.lang.Specification
import spock.lang.Unroll

import java.time.*

class TimeUtilsSpec extends Specification {

    void "getFormattedDuration, when input duration is missing, return empty String"() {
        expect:
        TimeUtils.getFormattedDuration(null) == ""
    }

    void "getFormattedDurationWithDays, when d1 is missing, return empty String"() {
        expect:
        TimeUtils.getFormattedDurationWithDays(null, new Date()) == ""
    }

    void "getFormattedDurationWithDays, when d2 is missing, return empty String"() {
        expect:
        TimeUtils.getFormattedDurationWithDays(new Date(), null) == ""
    }

    void "getFormattedDurationWithDays, when both provided, then return expected string"() {
        expect:
        TimeUtils.getFormattedDurationWithDays(new Date(), new Date() + 1) == "1d 00h 00m"
    }

    @Unroll
    void "getFormattedDurationWithDays, when called, then return formatted string '#expected'"() {
        given:
        LocalDateTime start = LocalDateTime.now()
        LocalDateTime end = start.plusDays(dayOffset).plusHours(hourOffset).plusMinutes(minuteOffset)

        when:
        String duration = TimeUtils.getFormattedDurationWithDays(start, end)

        then:
        duration == expected

        where:
        dayOffset | hourOffset | minuteOffset || expected
        1         | 0          | 0            || "1d 00h 00m"
        -1        | 0          | 0            || "-1d 00h 00m"
        14        | 12         | 2            || "14d 12h 02m"
        0         | 3          | 7            || "0d 03h 07m"
        0         | 0          | 17           || "0d 00h 17m"
        0         | 0          | 0            || "0d 00h 00m"
        0         | 0          | -17          || "-0d 00h 17m"
    }

    @Unroll
    void "getFormattedDuration, when called with ZonedDateTime params, then return formatted string '#expected'"() {
        given:
        ZonedDateTime start = ZonedDateTime.now()
        ZonedDateTime end = start.plusHours(hourOffset).plusMinutes(minuteOffset).plusSeconds(secondOffset)

        when:
        String duration = TimeUtils.getFormattedDurationForZonedDateTime(start, end)

        then:
        duration == expected

        where:
        hourOffset | minuteOffset | secondOffset || expected
        0          | 0            | 0            || "00:00:00"
        12         | 0            | 0            || "12:00:00"
        0          | 12           | 0            || "00:12:00"
        0          | 0            | 12           || "00:00:12"
        12         | 34           | 56           || "12:34:56"
    }

    @Unroll
    void "fromMillis, toMillis, when #value is convert to zonedDateTime and back, the value is the same"() {
        when:
        ZonedDateTime zonedDateTime = TimeUtils.fromMillis(value)
        long millis = TimeUtils.toMillis(zonedDateTime)

        then:
        millis == value

        where:
        value << [
                0,
                49,
                100,
                456789,
                1000 * 60 * 60 * 24 * 500,
        ]
    }

    @Unroll
    void "toLocalDateTime, when date is #year #month #day #hour #minutes #seconds, then convert it correct"() {
        when:
        Date date = new Date(year - 1900, month - 1, day, hour, minutes, seconds)
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minutes, seconds)

        then:
        localDateTime == TimeUtils.toLocalDateTime(date)

        where:
        year | month | day | hour | minutes | seconds
        2000 | 1     | 1   | 0    | 0       | 0
        2018 | 3     | 14  | 15   | 14      | 13
        2000 | 2     | 29  | 9    | 7       | 45
    }

    @Unroll
    void "toZonedDateTime, when date is #year #month #day #hour #minutes #seconds, then convert it correct"() {
        when:
        Date date = new Date(year - 1900, month - 1, day, hour, minutes, seconds)
        ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, day, hour, minutes, seconds, 0, ZoneId.systemDefault())

        then:
        zonedDateTime == TimeUtils.toZonedDateTime(date)

        where:
        year | month | day | hour | minutes | seconds
        2000 | 1     | 1   | 0    | 0       | 0
        2018 | 3     | 14  | 15   | 14      | 13
        2000 | 2     | 29  | 9    | 7       | 45
    }

    @Unroll
    void "toDate, when date is #year #month #day,  then convert it correct"() {
        when:
        LocalDate localDate = LocalDate.of(year, month, day)
        Date date = new Date(year - 1900, month - 1, day)

        then:
        date == TimeUtils.toDate(localDate)

        where:
        year | month | day
        2000 | 1     | 1
        2018 | 3     | 14
        2000 | 2     | 29
    }
}
