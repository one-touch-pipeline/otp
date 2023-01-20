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

import groovy.transform.CompileDynamic

import java.time.*
import java.time.format.DateTimeFormatter

enum TimeFormats {
    DATE('yyyy-MM-dd'),
    DATE_DIRECTORY('yyyy/MM/dd'),
    TIME('HH:mm:ss'),
    TIME_DASHED('HH-mm-ss'),
    DATE_TIME('yyyy-MM-dd HH:mm:ss'),
    DATE_TIME_WITHOUT_SECONDS('yyyy-MM-dd HH:mm'),
    MONTH_YEAR('MMM yyyy'),
    YEAR_MONTH_SLASH('yyyy/MM'),
    DATE_TIME_WITH_ZONE('yyyy-MM-dd-HH-mm-ss-SSSZ'),
    WEEKDAY_DATE_TIME('EEE, d MMM yyyy HH:mm'),
    DATE_2('dd.MM.yyyy'),
    SHORT_DATE('dd.MM.'),
    TIME_WEEKDAY_DATE('HH:mm:ss E dd.MM.yyyy'),
    TIME_DATE('HH:mm:ss dd.MM.yyyy'),
    TIME_SHORT_DATE('HH:mm:ss dd.MM.'),
    DATE_TIME_SECONDS('yyyy-MM-dd-HHmmssSSSS'),
    DATE_TIME_SECONDS_DASHES('yyyy-MM-dd-HH-mm-ss-SSS'),
    DATE_TIME_DASHES('yyyy-MM-dd-HH-mm-ss')

    final String format

    private TimeFormats(String format) {
        this.format = format
    }

    String getFormattedZonedDateTime(ZonedDateTime date, Locale locale = Locale.ENGLISH) {
        return date ? date.format(DateTimeFormatter.ofPattern(format, locale)) : 'na'
    }

    String getFormattedLocalDate(LocalDate date, Locale locale = Locale.ENGLISH) {
        return date ? date.format(DateTimeFormatter.ofPattern(format, locale)) : 'na'
    }

    String getFormattedDate(Date date, Locale locale = Locale.ENGLISH) {
        return date ? TimeUtils.toZonedDateTime(date).format(DateTimeFormatter.ofPattern(format, locale)) : 'na'
    }

    String getFormattedLocalDateTime(LocalDateTime localDateTime, Locale locale = Locale.ENGLISH) {
        return localDateTime ? localDateTime.format(DateTimeFormatter.ofPattern(format, locale)) : 'na'
    }
    /**
     * Formats the date in such a way that only relevant changed values are displayed.
     *
     * Same day         : hh:mm:ss
     * Same year        : hh-mm-ss DD.MM.
     * Totally different: hh-mm-ss DD.MM.YYYY
     *
     * @param date Date object to format
     */
    static String getInShortestTimeFormat(Date date) {
        Date now = new Date()
        boolean same24hours = ((now.time - date.time) <= 86400000) // 86.400.000 = 24h * 60m * 60s * 1000ms
        if (same24hours) {
            return TIME.getFormattedDate(date)
        } else if (now.year == date.year) {
            return TIME_SHORT_DATE.getFormattedDate(date)
        }
        return TIME_DATE.getFormattedDate(date)
    }

    /**
     * Helper method to render a date in a common way.
     * @param value Date in JSON representation
     * @returns Formatted dates and value to sort by
     */
    @CompileDynamic
    static Map<String, Object> asTimestamp(Date date) {
        return date ? [
                shortest: getInShortestTimeFormat(date),
                full    : TIME_WEEKDAY_DATE.getFormattedDate(date),
                value   : date.time,
        ] : [shortest: "-", full: "-", value: 0]
    }
}
