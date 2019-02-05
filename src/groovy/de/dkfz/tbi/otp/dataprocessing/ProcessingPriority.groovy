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

package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.TupleConstructor

/**
 * Data with a higher processing priority value should be processed before data with a lower processing priority value.
 */
@TupleConstructor
enum ProcessingPriority {

    MINIMUM(Short.MIN_VALUE),

    NORMAL(0 as short),

    FAST_TRACK(10000 as short),

    MAXIMUM(Short.MAX_VALUE - 1 as short),

    /**
     * A priority value which is strictly greater than the processing value of all data.
     */
    SUPREMUM(Short.MAX_VALUE),

    final short priority

    static final List<ProcessingPriority> displayPriorities = [MINIMUM, NORMAL, FAST_TRACK]

    static getByPriorityNumber(short priority) {
        return values().find { it.priority == priority }
    }
}
