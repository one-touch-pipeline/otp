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
package de.dkfz.tbi.otp.job.plan

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity

/**
 * The JobDecision defines a possible outcome of a {@link DecisionJob}.
 * It does not contain any information about the next {@link JobDefinition}
 * to be executed.
 *
 * @see JobDefinition
 * @see DecidingJobDefinition
 * @see DecisionJob
 * @see DecisionMapping
 * @deprecated class is part of the old workflow system
 */
@Deprecated
@ManagedEntity
class JobDecision implements Entity {
    /**
     * The JobDefinition this decision belongs to. It is not the definition the decision points to!
     */
    DecidingJobDefinition jobDefinition
    /**
     * The name of the decision. May be used by the Job Implementation to set it's decision.
     */
    String name
    /**
     * A descriptive name for the usage in the interface.
     */
    String description

    static constraints = {
        jobDefinition(nullable: false)
        name(nullable: false, blank: false, unique: 'jobDefinition')
        description(nullable: false, blank: true)
    }
}
