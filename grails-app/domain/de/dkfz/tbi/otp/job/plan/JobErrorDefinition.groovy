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
package de.dkfz.tbi.otp.job.plan

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * @deprecated class is part of the old workflow system
 */
@Deprecated
@ManagedEntity
class JobErrorDefinition implements Entity {
    String errorExpression
    Type type
    Action action

    /**
     * This enum defines the source of the error message.
     */
    enum Type {
        MESSAGE,
        STACKTRACE,
        CLUSTER_LOG
    }
    /**
     * This enum defines how to handle a failed job.
     */
    enum Action {
        RESTART_WF,
        RESTART_JOB,
        STOP,
        CHECK_FURTHER,
    }

    static constraints = {
        errorExpression(validator: { val, obj ->
            try {
                Pattern.compile(val)
            }
            catch (PatternSyntaxException e) {
                return "invalid"
            }
            return true
        })
    }
    static hasMany = [
            checkFurtherJobErrors: JobErrorDefinition,
            jobDefinitions       : JobDefinition,
    ]

    static mapping = {
        errorExpression type: 'text'
    }

    @Override
    String toString() {
        return "JobErrorDefinition(id=${id}, type=${type}, action=${action}, errorExpression=${errorExpression})"
    }
}
