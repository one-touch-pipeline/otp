/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.wes

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.util.TimeFormats

import java.time.ZonedDateTime

@ManagedEntity
class WesLog implements Entity {
    String name
    String cmd
    ZonedDateTime startTime
    ZonedDateTime endTime
    String stdout
    String stderr
    Integer exitCode

    static Closure constraints = {
        cmd nullable: true
        stdout nullable: true
        stderr nullable: true
        endTime nullable: true
        startTime nullable: true
        exitCode nullable: true
    }

    static Closure mapping = {
        stdout type: "text"
        stderr type: "text"
        cmd type: "text"
    }

    @Override
    String toString() {
        return """\
WesLog{
    id=$id,
    name='$name',
    startTime=${TimeFormats.DATE_TIME_SECONDS.getFormattedZonedDateTime(startTime)},
    endTime=${TimeFormats.DATE_TIME_SECONDS.getFormattedZonedDateTime(endTime)},
    exitCode=$exitCode,
    stdout='$stdout',
    stderr='$stderr',
    cmd='$cmd'
}"""
    }
}
