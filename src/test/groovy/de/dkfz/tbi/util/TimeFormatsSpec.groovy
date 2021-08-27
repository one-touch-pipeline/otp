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

class TimeFormatsSpec extends Specification {

    @Unroll
    void "getFormatted, when ZonedDateTime #date, then return expected formats"() {
        expect:
        TimeFormats.DATE.getFormatted(date) == formatDate
        TimeFormats.TIME.getFormatted(date) == formatTime
        TimeFormats.DATE_TIME.getFormatted(date) == formatDateTime
        TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(date) == formatDateTimeWithoutSeconds
        TimeFormats.MONTH_YEAR.getFormatted(date) == formatMonthYear
        TimeFormats.DATE_TIME_WITH_ZONE.getFormatted(date) == formatDateTimeWithZone
        TimeFormats.WEEKDAY_DATE_TIME.getFormatted(date) == formatWeekdayDateTime
        TimeFormats.DATE_2.getFormatted(date) == formatDate2
        TimeFormats.SHORT_DATE.getFormatted(date) == formatShortDate
        TimeFormats.TIME_WEEKDAY_DATE.getFormatted(date) == formatTimeWeekdayDate
        TimeFormats.TIME_DATE.getFormatted(date) == formatTimeDate
        TimeFormats.TIME_SHORT_DATE.getFormatted(date) == formatTimeShortDate

        where:
        date                                                                      | formatDate   | formatTime | formatDateTime        | formatDateTimeWithoutSeconds | formatMonthYear | formatDateTimeWithZone         | formatWeekdayDateTime    | formatDate2  | formatShortDate | formatTimeWeekdayDate     | formatTimeDate        | formatTimeShortDate
        ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of('+0000'))              | '2000-01-01' | '00:00:00' | '2000-01-01 00:00:00' | '2000-01-01 00:00'           | 'Jan 2000'      | '2000-01-01-00-00-00-000+0000' | 'Sat, 1 Jan 2000 00:00'  | '01.01.2000' | '01.01.'        | '00:00:00 Sat 01.01.2000' | '00:00:00 01.01.2000' | '00:00:00 01.01.'
        ZonedDateTime.of(2000, 05, 15, 16, 23, 48, 0, ZoneId.of('+0000'))         | '2000-05-15' | '16:23:48' | '2000-05-15 16:23:48' | '2000-05-15 16:23'           | 'May 2000'      | '2000-05-15-16-23-48-000+0000' | 'Mon, 15 May 2000 16:23' | '15.05.2000' | '15.05.'        | '16:23:48 Mon 15.05.2000' | '16:23:48 15.05.2000' | '16:23:48 15.05.'
        ZonedDateTime.of(2000, 12, 31, 23, 59, 59, 999999999, ZoneId.of('+0000')) | '2000-12-31' | '23:59:59' | '2000-12-31 23:59:59' | '2000-12-31 23:59'           | 'Dec 2000'      | '2000-12-31-23-59-59-999+0000' | 'Sun, 31 Dec 2000 23:59' | '31.12.2000' | '31.12.'        | '23:59:59 Sun 31.12.2000' | '23:59:59 31.12.2000' | '23:59:59 31.12.'
    }

    @Unroll
    void "getFormatted, when LocalDateTime #date, then return expected formats"() {
        expect:
        TimeFormats.DATE.getFormatted(date) == formatDate
        TimeFormats.MONTH_YEAR.getFormatted(date) == formatMonthYear
        TimeFormats.DATE_2.getFormatted(date) == formatDate2

        where:
        date                       | formatDate   | formatMonthYear | formatDate2
        LocalDate.of(2000, 1, 1)   | '2000-01-01' | 'Jan 2000'      | '01.01.2000'
        LocalDate.of(2000, 05, 15) | '2000-05-15' | 'May 2000'      | '15.05.2000'
        LocalDate.of(2000, 12, 31) | '2000-12-31' | 'Dec 2000'      | '31.12.2000'
    }

    @Unroll
    void "getFormatted, when Date #date, then return expected formats"() {
        expect:
        TimeFormats.DATE.getFormatted(date) == formatDate
        TimeFormats.TIME.getFormatted(date) == formatTime
        TimeFormats.DATE_TIME.getFormatted(date) == formatDateTime
        TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormatted(date) == formatDateTimeWithoutSeconds
        TimeFormats.MONTH_YEAR.getFormatted(date) == formatMonthYear
        TimeFormats.WEEKDAY_DATE_TIME.getFormatted(date) == formatWeekdayDateTime
        TimeFormats.DATE_2.getFormatted(date) == formatDate2
        TimeFormats.SHORT_DATE.getFormatted(date) == formatShortDate
        TimeFormats.TIME_WEEKDAY_DATE.getFormatted(date) == formatTimeWeekdayDate
        TimeFormats.TIME_DATE.getFormatted(date) == formatTimeDate
        TimeFormats.TIME_SHORT_DATE.getFormatted(date) == formatTimeShortDate

        where:
        date                                          | formatDate   | formatTime | formatDateTime        | formatDateTimeWithoutSeconds | formatMonthYear | formatWeekdayDateTime    | formatDate2  | formatShortDate | formatTimeWeekdayDate     | formatTimeDate        | formatTimeShortDate
        new Date(2000 - 1900, 1 - 1, 1, 0, 0, 0)      | '2000-01-01' | '00:00:00' | '2000-01-01 00:00:00' | '2000-01-01 00:00'           | 'Jan 2000'      | 'Sat, 1 Jan 2000 00:00'  | '01.01.2000' | '01.01.'        | '00:00:00 Sat 01.01.2000' | '00:00:00 01.01.2000' | '00:00:00 01.01.'
        new Date(2000 - 1900, 05 - 1, 15, 16, 23, 48) | '2000-05-15' | '16:23:48' | '2000-05-15 16:23:48' | '2000-05-15 16:23'           | 'May 2000'      | 'Mon, 15 May 2000 16:23' | '15.05.2000' | '15.05.'        | '16:23:48 Mon 15.05.2000' | '16:23:48 15.05.2000' | '16:23:48 15.05.'
        new Date(2000 - 1900, 12 - 1, 31, 23, 59, 59) | '2000-12-31' | '23:59:59' | '2000-12-31 23:59:59' | '2000-12-31 23:59'           | 'Dec 2000'      | 'Sun, 31 Dec 2000 23:59' | '31.12.2000' | '31.12.'        | '23:59:59 Sun 31.12.2000' | '23:59:59 31.12.2000' | '23:59:59 31.12.'
    }

    @Unroll
    void "asTimestamp, when Date #date, then return expected"() {
        expect:
        TimeFormats.asTimestamp(date)['shortest'] == formatDateShortest
        TimeFormats.asTimestamp(date)['full'] == formatDateFull

        where:
        date                                          | formatDateShortest    | formatDateFull
        new Date(2000 - 1900, 1 - 1, 1, 0, 0, 0)      | '00:00:00 01.01.2000' | '00:00:00 Sat 01.01.2000'
        new Date(2000 - 1900, 05 - 1, 15, 16, 23, 48) | '16:23:48 15.05.2000' | '16:23:48 Mon 15.05.2000'
        new Date(2000 - 1900, 12 - 1, 31, 23, 59, 59) | '23:59:59 31.12.2000' | '23:59:59 Sun 31.12.2000'
    }
}
