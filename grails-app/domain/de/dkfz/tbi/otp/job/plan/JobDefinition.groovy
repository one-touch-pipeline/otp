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

import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.utils.Entity

/**
 * @deprecated class is part of the old workflow system
 */
@Deprecated
@ManagedEntity
class JobDefinition implements Serializable, Entity {
    /**
     * Mapping of parameters.
     */
    Collection parameterMappings
    static hasMany = [constantParameters: Parameter, parameterMappings: ParameterMapping]
    /**
     * The JobExecutionPlan this JobDefinition belongs to.
     */
    JobExecutionPlan plan
    /**
     * The previous JobDefinition in the execution plan before this one.
     * For the first JobDefinition this is null.
     */
    JobDefinition previous
    /**
     * The next JobDefinition in the execution plan after this one.
     * For the last JobDefinition this is null.
     */
    JobDefinition next
    /**
     * A descriptive name for this JobDefinition
     */
    String name
    /**
     * The name of the Spring Bean which implements the Job to be executed for this JobDefinition
     */
    String bean

    static constraints = {
        name(nullable: false, blank: false, unique: 'plan')
        bean(nullable: false, blank: false)
        previous(nullable: true)
        next(nullable: true)
        plan(nullable: false)
    }

    static Closure mapping = {
        plan index: 'job_definition_plan_idx'
        previous index: 'job_definition_previous_idx'
        next index: 'job_definition_next_idx'
        bean index: 'job_definition_bean_idx'
    }
}
