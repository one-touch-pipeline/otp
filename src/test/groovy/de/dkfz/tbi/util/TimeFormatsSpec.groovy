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
    void "getFormattedZonedDateTime, when ZonedDateTime #date, then return expected formats"() {
        expect:
        TimeFormats.DATE.getFormattedZonedDateTime(date) == formatDate
        TimeFormats.TIME.getFormattedZonedDateTime(date) == formatTime
        TimeFormats.DATE_TIME.getFormattedZonedDateTime(date) == formatDateTime
        TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(date) == formatDateTimeWithoutSeconds
        TimeFormats.MONTH_YEAR.getFormattedZonedDateTime(date) == formatMonthYear
        TimeFormats.DATE_TIME_WITH_ZONE.getFormattedZonedDateTime(date) == formatDateTimeWithZone
        TimeFormats.WEEKDAY_DATE_TIME.getFormattedZonedDateTime(date) == formatWeekdayDateTime
        TimeFormats.DATE_2.getFormattedZonedDateTime(date) == formatDate2
        TimeFormats.SHORT_DATE.getFormattedZonedDateTime(date) == formatShortDate
        TimeFormats.TIME_WEEKDAY_DATE.getFormattedZonedDateTime(date) == formatTimeWeekdayDate
        TimeFormats.TIME_DATE.getFormattedZonedDateTime(date) == formatTimeDate
        TimeFormats.TIME_SHORT_DATE.getFormattedZonedDateTime(date) == formatTimeShortDate
        TimeFormats.DATE_TIME_SECONDS.getFormattedZonedDateTime(date) == formatDateTimeSeconds
        TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedZonedDateTime(date) == formatDateTimeSecondsDashes
        TimeFormats.DATE_TIME_DASHES.getFormattedZonedDateTime(date) == formatDateTimeDashes

        where:
        date                                                                      | formatDate   | formatTime | formatDateTime        | formatDateTimeWithoutSeconds | formatMonthYear | formatDateTimeWithZone         | formatWeekdayDateTime    | formatDate2  | formatShortDate | formatTimeWeekdayDate     | formatTimeDate        | formatTimeShortDate | formatDateTimeSeconds   | formatDateTimeSecondsDashes | formatDateTimeDashes
        ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of('+0000'))              | '2000-01-01' | '00:00:00' | '2000-01-01 00:00:00' | '2000-01-01 00:00'           | 'Jan 2000'      | '2000-01-01-00-00-00-000+0000' | 'Sat, 1 Jan 2000 00:00'  | '01.01.2000' | '01.01.'        | '00:00:00 Sat 01.01.2000' | '00:00:00 01.01.2000' | '00:00:00 01.01.'   | '2000-01-01-0000000000' | '2000-01-01-00-00-00-000'   | '2000-01-01-00-00-00'
        ZonedDateTime.of(2000, 05, 15, 16, 23, 48, 0, ZoneId.of('+0000'))         | '2000-05-15' | '16:23:48' | '2000-05-15 16:23:48' | '2000-05-15 16:23'           | 'May 2000'      | '2000-05-15-16-23-48-000+0000' | 'Mon, 15 May 2000 16:23' | '15.05.2000' | '15.05.'        | '16:23:48 Mon 15.05.2000' | '16:23:48 15.05.2000' | '16:23:48 15.05.'   | '2000-05-15-1623480000' | '2000-05-15-16-23-48-000'   | '2000-05-15-16-23-48'
        ZonedDateTime.of(2000, 12, 31, 23, 59, 59, 999999999, ZoneId.of('+0000')) | '2000-12-31' | '23:59:59' | '2000-12-31 23:59:59' | '2000-12-31 23:59'           | 'Dec 2000'      | '2000-12-31-23-59-59-999+0000' | 'Sun, 31 Dec 2000 23:59' | '31.12.2000' | '31.12.'        | '23:59:59 Sun 31.12.2000' | '23:59:59 31.12.2000' | '23:59:59 31.12.'   | '2000-12-31-2359599999' | '2000-12-31-23-59-59-999'   | '2000-12-31-23-59-59'
    }

    @Unroll
    void "getFormattedLocalDate, when LocalDate #date, then return expected formats"() {
        expect:
        TimeFormats.DATE.getFormattedLocalDate(date) == formatDate
        TimeFormats.MONTH_YEAR.getFormattedLocalDate(date) == formatMonthYear
        TimeFormats.DATE_2.getFormattedLocalDate(date) == formatDate2

        where:
        date                       | formatDate   | formatMonthYear | formatDate2
        LocalDate.of(2000, 1, 1)   | '2000-01-01' | 'Jan 2000'      | '01.01.2000'
        LocalDate.of(2000, 05, 15) | '2000-05-15' | 'May 2000'      | '15.05.2000'
        LocalDate.of(2000, 12, 31) | '2000-12-31' | 'Dec 2000'      | '31.12.2000'
    }

    @Unroll
    void "getFormattedDate, when Date #date, then return expected formats"() {
        expect:
        TimeFormats.DATE.getFormattedDate(date) == formatDate
        TimeFormats.TIME.getFormattedDate(date) == formatTime
        TimeFormats.DATE_TIME.getFormattedDate(date) == formatDateTime
        TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(date) == formatDateTimeWithoutSeconds
        TimeFormats.MONTH_YEAR.getFormattedDate(date) == formatMonthYear
        TimeFormats.WEEKDAY_DATE_TIME.getFormattedDate(date) == formatWeekdayDateTime
        TimeFormats.DATE_2.getFormattedDate(date) == formatDate2
        TimeFormats.SHORT_DATE.getFormattedDate(date) == formatShortDate
        TimeFormats.TIME_WEEKDAY_DATE.getFormattedDate(date) == formatTimeWeekdayDate
        TimeFormats.TIME_DATE.getFormattedDate(date) == formatTimeDate
        TimeFormats.TIME_SHORT_DATE.getFormattedDate(date) == formatTimeShortDate
        TimeFormats.DATE_TIME_SECONDS.getFormattedDate(date) == formatDateTimeSeconds
        TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(date) == formatDateTimeSecondsDashes
        TimeFormats.DATE_TIME_DASHES.getFormattedDate(date) == formatDateTimeDashes

        where:
        date                                          | formatDate   | formatTime | formatDateTime        | formatDateTimeWithoutSeconds | formatMonthYear | formatWeekdayDateTime    | formatDate2  | formatShortDate | formatTimeWeekdayDate     | formatTimeDate        | formatTimeShortDate | formatDateTimeSeconds   | formatDateTimeSecondsDashes | formatDateTimeDashes
        new Date(2000 - 1900, 1 - 1, 1, 0, 0, 0)      | '2000-01-01' | '00:00:00' | '2000-01-01 00:00:00' | '2000-01-01 00:00'           | 'Jan 2000'      | 'Sat, 1 Jan 2000 00:00'  | '01.01.2000' | '01.01.'        | '00:00:00 Sat 01.01.2000' | '00:00:00 01.01.2000' | '00:00:00 01.01.'   | '2000-01-01-0000000000' | '2000-01-01-00-00-00-000'   | '2000-01-01-00-00-00'
        new Date(2000 - 1900, 05 - 1, 15, 16, 23, 48) | '2000-05-15' | '16:23:48' | '2000-05-15 16:23:48' | '2000-05-15 16:23'           | 'May 2000'      | 'Mon, 15 May 2000 16:23' | '15.05.2000' | '15.05.'        | '16:23:48 Mon 15.05.2000' | '16:23:48 15.05.2000' | '16:23:48 15.05.'   | '2000-05-15-1623480000' | '2000-05-15-16-23-48-000'   | '2000-05-15-16-23-48'
        new Date(2000 - 1900, 12 - 1, 31, 23, 59, 59) | '2000-12-31' | '23:59:59' | '2000-12-31 23:59:59' | '2000-12-31 23:59'           | 'Dec 2000'      | 'Sun, 31 Dec 2000 23:59' | '31.12.2000' | '31.12.'        | '23:59:59 Sun 31.12.2000' | '23:59:59 31.12.2000' | '23:59:59 31.12.'   | '2000-12-31-2359590000' | '2000-12-31-23-59-59-000'   | '2000-12-31-23-59-59'
    }

    @Unroll
    void "getFormattedLocalDateTime, when LocalDateTime #date, then return expected formats"() {
        expect:
        TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedLocalDateTime(date) == formatLocalDateTime

        where:
        date                                                                         | formatLocalDateTime
        LocalDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0, 0, 0))      | '2000-01-01-00-00-00-000'
        LocalDateTime.of(LocalDate.of(2000, 5, 15), LocalTime.of(16, 23, 48, 0))  | '2000-05-15-16-23-48-000'
        LocalDateTime.of(LocalDate.of(2000, 12, 31), LocalTime.of(23, 59, 59, 0)) | '2000-12-31-23-59-59-000'
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
