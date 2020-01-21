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
package de.dkfz.tbi.otp.job.jobs.utils

import de.dkfz.tbi.otp.job.processing.AbstractMultiJob

/**
 * Contains some valid keys for job input and output parameters.
 */
class JobParameterKeys {

    // The values of all constants should be unique. Non-unique values might lead to confusion and bad things happening.

    /**
     * @deprecated Create/use a subclass of {@link AbstractMultiJob}, then cluster job IDs no longer have to be passed
     * between jobs.
     */
    @Deprecated
    static final String JOB_ID_LIST = '__pbsIds'
    /**
     * @deprecated Create/use a subclass of {@link AbstractMultiJob}, then cluster job IDs no longer have to be passed
     * between jobs.
     */
    @Deprecated
    static final String REALM = '__pbsRealm'
    static final String SCRIPT = 'SCRIPT'
}
