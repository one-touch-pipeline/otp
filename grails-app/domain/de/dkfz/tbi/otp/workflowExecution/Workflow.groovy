/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.Entity

import java.time.LocalDate

class Workflow implements Entity, Commentable {

    String name

    String beanName

    boolean enabled

    LocalDate deprecatedDate

    WesServer wesServer

    int maxParallelRunning

    Set<ReferenceGenome> allowedReferenceGenomes

    Set<SeqType> supportedSeqTypes

    static constraints = {
        beanName nullable: true
        deprecatedDate nullable: true
        wesServer nullable: true
        maxParallelRunning shared: 'greaterThanZero'
        comment nullable: true
    }

    @Override
    String toString() {
        "${name}${deprecatedDate ? " (deprecated)" : ""}"
    }

    static hasMany = [
            allowedReferenceGenomes: ReferenceGenome,
            supportedSeqTypes      : SeqType,
    ]
}
