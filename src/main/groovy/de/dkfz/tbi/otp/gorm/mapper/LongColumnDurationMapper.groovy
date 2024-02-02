/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.gorm.mapper

import org.jadira.usertype.spi.shared.AbstractLongColumnMapper

import java.time.Duration

/**
 * Converts java-time {@link Duration} objects to long and vice versa
 */
class LongColumnDurationMapper extends AbstractLongColumnMapper<Duration> {

    @Override
    Long toNonNullValue(Duration duration) {
        return duration.toMillis()
    }

    @Override
    String toNonNullString(Duration duration) {
        return toNonNullValue(duration).toString()
    }

    @Override
    Duration fromNonNullValue(Long millis) {
        return Duration.ofMillis(millis)
    }

    @Override
    Duration fromNonNullString(String millisAsString) {
        return fromNonNullValue(millisAsString as long)
    }
}
