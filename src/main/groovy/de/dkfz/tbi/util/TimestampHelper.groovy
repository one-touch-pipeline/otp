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
package de.dkfz.tbi.util

import java.text.SimpleDateFormat

class TimestampHelper {

    private static final String TIME_PATTERN = "HH:mm:ss"
    private static final String DATE_PATTERN = "dd.MM.yyyy"
    private static final String SHORT_DATE_PATTERN = "dd.MM."

    private static final String TIME_WEEKDAY_DATE = "${TIME_PATTERN} E ${DATE_PATTERN}"
    private static final String TIME_DATE = "${TIME_PATTERN} ${DATE_PATTERN}"
    private static final String TIME_SHORT_DATE = "${TIME_PATTERN} ${SHORT_DATE_PATTERN}"

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
        boolean same24hours = ((now.getTime() - date.getTime()) <= 86400000) // 86.400.000 = 24h * 60m * 60s * 1000ms
        if (same24hours) {
            return new SimpleDateFormat(TIME_PATTERN, Locale.ENGLISH).format(date)
        } else {
            if (now.getYear() == date.getYear()) {
                new SimpleDateFormat(TIME_SHORT_DATE, Locale.ENGLISH).format(date)
            } else {
                new SimpleDateFormat(TIME_DATE, Locale.ENGLISH).format(date)
            }
        }
    }

    /**
     * Helper method to render a date in a common way.
     * @param value Date in JSON representation
     * @returns Formatted dates and value to sort by
     */
    static Map<String, Object> asTimestamp(Date date) {
        if (!date) {
            return [shortest: "-", full: "-", value: 0]
        }
        return [
                shortest: getInShortestTimeFormat(date),
                full    : new SimpleDateFormat(TIME_WEEKDAY_DATE, Locale.ENGLISH).format(date),
                value   : date.getTime(),
        ]
    }
}
