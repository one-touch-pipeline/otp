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

import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.utils.Entity

/**
 * @deprecated class is part of the old workflow system
 */
@Deprecated
@ManagedEntity
class JobExecutionPlan implements Serializable, Entity {
    /**
     * A descriptive name for this Job Execution Plan
     */
    String name
    /**
     * The previous version of this Job Execution Plan
     */
    JobExecutionPlan previousPlan
    /**
     * The version Number for this version of the Plan
     */
    int planVersion
    /**
     * Whether the Plan is obsoleted, that is there is a Job Execution Plan
     * which has this Plan as its previousPlan.
     */
    boolean obsoleted
    /**
     * The Job Definition for the first Job to be run when a Process gets created.
     */
    JobDefinition firstJob
    /**
     * The JobDefinition describing the StartJob
     */
    StartJobDefinition startJob
    /**
     * Whether this Job Execution Plan is currently enabled or disabled. If it is disabled
     * no new Job will be started for this Plan.
     */
    boolean enabled
    /**
     * The parameter which is static for the process
     */
    ProcessParameter processParameter

    static constraints = {
        name(nullable: false, blank: false, unique: 'planVersion')
        planVersion(min: 0)
        previousPlan(nullable: true, validator: { JobExecutionPlan value, JobExecutionPlan current ->
            if (value) {
                return current.planVersion > 0 && value.name == current.name
            }
            return current.planVersion == 0
        })
        // firstJob needs to be nullable as JobDefinition has a dependency on JobExecutionPlan and this circle could not be solved in the database
        firstJob(nullable: true)
        // firstJob needs to be nullable as JobDefinition has a dependency on JobExecutionPlan and this circle could not be solved in the database
        startJob(nullable: true)
        processParameter(nullable: true, blank: true)
    }

    static Closure mapping = {
        previousPlan index: 'job_execution_plan_previous_plan_idx'
        firstJob index: 'job_execution_plan_first_job_idx'
        startJob index: 'job_execution_start_job_idx'
        processParameter index: 'job_execution_process_parameter_idx'
    }

    @Override
    String toString() {
        return this.name
    }
}
