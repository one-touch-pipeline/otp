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

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum TimeFormats {
    DATE('yyyy-MM-dd'),
    TIME ('HH:mm:ss'),
    DATE_TIME ('yyyy-MM-dd HH:mm:ss'),
    DATE_TIME_WITHOUT_SECONDS('yyyy-MM-dd HH:mm'),
    MONTH_YEAR ('MMM yyyy'),
    DATE_TIME_WITH_ZONE('yyyy-MM-dd-HH-mm-ss-SSSZ'),

    final String format

    final DateTimeFormatter formatter

    private TimeFormats(String format) {
        this.format = format
        formatter = DateTimeFormatter.ofPattern(format)
    }

    String getFormatted(ZonedDateTime date) {
        return date ? date.format(formatter) : 'na'
    }

    String getFormatted(LocalDate date) {
        return date ? date.format(formatter) : 'na'
    }

    String getFormatted(Date date) {
        return date ? TimeUtils.toZonedDateTime(date).format(formatter) : 'na'
    }
}
