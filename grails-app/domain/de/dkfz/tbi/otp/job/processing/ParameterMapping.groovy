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
package de.dkfz.tbi.otp.job.processing

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.utils.Entity

/**
 * The ParameterMapping describes the mapping of an output {@link Parameter}
 * to the input Parameter of the next {@link JobDefinition}.
 *
 * @deprecated class is part of the old workflow system
 */
@Deprecated
@ManagedEntity
class ParameterMapping implements Entity {
    /**
     * The ParameterType of the output Parameter of the previous Job
     */
    ParameterType from
    /**
     * The ParameterType of the input Parameter of the next Job
     */
    ParameterType to
    /**
     * The Next Job to which this mapping belongs to.
     */
    JobDefinition job

    static constraints = {
        from(nullable: false, unique: 'to', validator: { ParameterType value, ParameterMapping mapping ->
            if (value.jobDefinition == mapping.to?.jobDefinition) {
                return "jobDefinition"
            }
            if (value.parameterUsage != ParameterUsage.OUTPUT && value.parameterUsage != ParameterUsage.PASSTHROUGH) {
                return "parameterUsage"
            }
        })
        to(nullable: false, unique: 'from', validator: { ParameterType value, ParameterMapping mapping ->
            if (value.jobDefinition == mapping.from?.jobDefinition) {
                return "jobDefinition"
            }
            if (value.parameterUsage != ParameterUsage.INPUT && value.parameterUsage != ParameterUsage.PASSTHROUGH) {
                return "parameterUsage"
            }
        })
        job(nullable: false, validator: { JobDefinition value, ParameterMapping mapping ->
            return value == mapping.to?.jobDefinition
        })
    }

    static Closure mapping = {
        from index: 'parameter_mapping_from_idx'
        to index: 'parameter_mapping_to_idx'
        job index: 'parameter_mapping_job_idx'
    }
}
