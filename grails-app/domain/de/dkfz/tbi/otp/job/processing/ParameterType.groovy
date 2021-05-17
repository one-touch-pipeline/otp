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

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.utils.Entity

/**
 * The ParameterType is the generic description for {@link Parameter}s of a {@link JobDefinition}.
 *
 * It contains a name which is unique for a JobDefinition, a human readable description
 * and whether it references a domain class or not. The actual values are Parameter
 * which combines a ParameterType with a value.
 *
 * The ParameterType allows to easily map the output Parameters from one JobDefinition to
 * the input Parameters of the next Job.
 *
 * @see Parameter
 * @see JobDefinition
 *
 * @deprecated class is part of the old workflow system
 */
@Deprecated
class ParameterType implements Serializable, Entity {
    /**
     * The name for this parameter
     */
    String name
    /**
     * A description for the parameter
     */
    String description
    /**
     * If not null it references the Domain Class the Parameter has as instance
     */
    String className
    /**
     * The JobDefinition for which this parameter type has been created
     */
    JobDefinition jobDefinition
    /**
     * The kind for which the Job is used
     */
    ParameterUsage parameterUsage

    static constraints = {
        name(nullable: false, blank: false, unique: ['jobDefinition', 'parameterUsage'])
        description(nullable: true)
        className(nullable: true)
        parameterUsage(nullable: false)
    }
}
