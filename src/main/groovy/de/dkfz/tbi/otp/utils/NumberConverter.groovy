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
package de.dkfz.tbi.otp.utils

import grails.databinding.converters.ValueConverter
import org.springframework.core.PriorityOrdered

class NumberConverter implements ValueConverter, PriorityOrdered {

    Class<?> targetType

    @Override
    boolean canConvert(Object value) {
        return value instanceof String
    }

    @Override
    Object convert(Object value1) {
        String value = value1.toString().trim()
        if (value.contains(",")) {
            throw new IllegalArgumentException("Numbers must not contain ','")
        }

        if (!value) {
            return null
        }

        if (targetType == Float || targetType == float) {
            return Float.parseFloat(value)
        } else if (targetType == Double || targetType == double) {
            return Double.valueOf(value)
        } else if (targetType == Short || targetType == short) {
            return Short.parseShort(value)
        } else if (targetType == Integer || targetType == int) {
            return Integer.parseInt(value)
        } else if (targetType == Long || targetType == long) {
            return Long.parseLong(value)
        } else if (targetType == BigInteger) {
            return new BigInteger(value)
        } else if (targetType == BigDecimal) {
            return new BigDecimal(value)
        }
    }

    final int order = HIGHEST_PRECEDENCE
}
