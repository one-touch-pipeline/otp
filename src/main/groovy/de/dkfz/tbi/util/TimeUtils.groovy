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

import java.text.SimpleDateFormat
import java.time.*
import java.util.concurrent.TimeUnit

class TimeUtils {

    /**
     * Convert a Duration into a String with the format "hh:mm:ss".
     *
     * @param duration to convert
     * @return String with format "hh:mm:ss"
     */
    static String getFormattedDuration(Duration duration) {
        if (!duration) {
            return ""
        }

        long millis = duration.toMillis()
        String negative = duration.isNegative() ? '-' : ''
        long absMillis = Math.abs(millis)

        return String.format("${negative}%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(absMillis),
                TimeUnit.MILLISECONDS.toMinutes(absMillis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(absMillis)),
                TimeUnit.MILLISECONDS.toSeconds(absMillis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(absMillis)))
    }

    static String getFormattedDuration(LocalDateTime start, LocalDateTime end) {
        if (!start || !end) {
            return ""
        }
        return getFormattedDuration(Duration.between(start, end))
    }

    /**
     * Convert a Duration into a String with the format "hh:mm:ss".
     *
     * @param start of the duration
     * @param end of the duration
     * @return String with format "hh:mm:ss"
     */
    static String getFormattedDurationForZonedDateTime(ZonedDateTime start, ZonedDateTime end) {
        if (!start || !end) {
            return null
        }

        return getFormattedDuration(Duration.between(start, end))
    }

    /**
     * Convert a Duration with days into a String with the format "dd hh mm".
     *
     * @param duration to convert
     * @return String with format "dd hh mm"
     */
    static String getFormattedDurationWithDays(Duration duration) {
        String negative = duration.isNegative() ? '-' : ''
        Duration positiveDuration = duration.isNegative() ? duration.negated() : duration

        long day = positiveDuration.toDays()
        long hours = positiveDuration.toHours() % 24
        long minutes = positiveDuration.toMinutes() % 60

        return String.format("${negative}%dd %02dh %02dm",
                day,
                hours,
                minutes
        )
    }

    /**
     * Convert a Duration with days into a String with the format "dd hh mm".
     *
     * @param start of the duration
     * @param end of the duration
     * @return String with format "dd hh mm"
     */
    static String getFormattedDurationWithDays(LocalDateTime start, LocalDateTime end) {
        if (!start || !end) {
            return null
        }

        return getFormattedDurationWithDays(Duration.between(start, end))
    }

    /**
     * Convert a Duration with days into a String with the format "dd hh mm".
     *
     * @param start of the duration
     * @param end of the duration
     * @return String with format "dd hh mm"
     */
    static String getFormattedDurationWithDays(Date start, Date end) {
        if (!start || !end) {
            return ""
        }
        return getFormattedDurationWithDays(toLocalDateTime(start), toLocalDateTime(end))
    }

    /**
     * Convert long into ZonedDateTime
     */
    static ZonedDateTime fromMillis(long millis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    }

    /**
     * Convert ZonedDateTime to milliseconds as long.
     */
    static long toMillis(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().toEpochMilli()
    }

    /**
     * Convert LocalDate to milliseconds as long.
     */
    static long toMillis(LocalDate dateTime) {
        return toMillis(ZonedDateTime.of(dateTime.year, dateTime.month.value, dateTime.dayOfMonth, 0, 0, 0, 0, ZoneId.systemDefault()))
    }

    /**
     * Convert LocalDate to Date.
     */
    static Date toDate(LocalDate dateTime) {
        return new Date(toMillis(dateTime))
    }

    /**
     * Convert Date to LocalDateTime.
     */
    static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
    }

    /**
     * Convert Date to ZonedDateTime.
     */
    static ZonedDateTime toZonedDateTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
    }

    /**
     * Tries to parse the text with a given format. Formats are defined by the TimeFormats class
     */
    static Date tryParseDate(TimeFormats format, String text) {
        Date date = null
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format.format, Locale.ENGLISH)
            date = simpleDateFormat.parse(text)
        } catch (Exception ignored) {
            // no exception
        }
        return date
    }

}
