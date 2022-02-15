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
package de.dkfz.tbi.otp.job.processing

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity

/**
 * Domain class to store error information for a failure ProcessingStepUpdate.
 *
 * An error can either be just a message provided by the Job itself or in cause of an exception
 * in which case the class also contains an identifier for the stack trace. The stack trace itself
 * is not stored in the database but should be stored to a log file by the container. The identifier
 * allows to find the stack trace in the corresponding log file by an administrator.
 *
 * @see ProcessingStepUpdate
 *
 * @deprecated class is part of the old workflow system
 */
@Deprecated
@ManagedEntity
class ProcessingError implements Entity {

    /**
     * The error message which can be shown in the user interface.
     */
    String errorMessage

    /**
     * A stack trace identifier to find the corresponding stack trace in a log file.
     * This could be a MD5 sum for example.
     *
     * If the error is not an exception this field should be {@code null}.
     */
    String stackTraceIdentifier

    static mapping = {
        errorMessage type: 'text'
    }

    static constraints = {
        errorMessage(nullable: false, blank: false)
        stackTraceIdentifier(nullable: true)
    }
}
