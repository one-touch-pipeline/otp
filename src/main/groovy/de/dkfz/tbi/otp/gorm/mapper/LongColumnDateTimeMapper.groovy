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
package de.dkfz.tbi.otp.gorm.mapper

import org.jadira.usertype.dateandtime.joda.util.ZoneHelper
import org.jadira.usertype.dateandtime.shared.spi.AbstractLongColumnMapper
import org.joda.time.DateTime

/**
 * Converts joda-time {@link org.joda.time.DateTime} objects to long and vice versa
 */
class LongColumnDateTimeMapper extends AbstractLongColumnMapper<DateTime> {

    @Override
    Long toNonNullValue(DateTime dateTime) {
        return dateTime.millis
    }

    @Override
    String toNonNullString(DateTime dateTime) {
        return toNonNullValue(dateTime).toString()
    }

    @Override
    DateTime fromNonNullValue(Long millis) {
        return new DateTime(millis).withZone(ZoneHelper.getDefault())
    }

    @Override
    DateTime fromNonNullString(String millisAsString) {
        return fromNonNullValue(millisAsString as long)
    }
}
