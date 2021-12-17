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

import grails.util.Environment
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.utils.Entity

/**
 * A ProcessingStep represents one execution of a Job for a JobDefinition.
 *
 * The ProcessingStep class is mostly just a logging facility to log which {@link Job} got
 * executed with which input parameters and what output parameters are generated. As well it
 * can be used to keep track of the status updates of the running Job. These are stored as
 * {@link ProcessingStepUpdate}s and added to this class.
 *
 * Each ProcessingStep belongs to one {@link Process} and was generated from a {@link JobDefinition}.
 * Based on the output parameters the framework will decide which is the next JobDefinition to use
 * to generate a new Job and ProcessingStep.
 *
 * @see Process
 * @see ProcessingStepUpdate
 * @see JobDefinition
 * @see Job
 * @see Parameter
 *
 * @deprecated class is part of the old workflow system
 */
@Deprecated
class ProcessingStep implements Serializable, Entity {
    /**
     * Input Parameters added to this ProcessingStep.
     */
    Collection<Parameter> input = [] as Collection<Parameter>
    /**
     * Output Parameters generated during this ProcessingStep.
     */
    Collection<Parameter> output = [] as Collection<Parameter>
    static hasMany = [input: Parameter, output: Parameter]
    /**
     * The JobDefinition this ProcessingStep is generated from.
     */
    JobDefinition jobDefinition
    /**
     * The name of the Job class which is used for this ProcessingStep.
     */
    String jobClass
    /**
     * The Process this ProcessingStep belongs to.
     */
    Process process
    /**
     * The previous ProcessingStep. Is null for the first ProcessingStep of a Process.
     */
    ProcessingStep previous
    /**
     * The next ProcessingStep. Is null as long as the processing step has not been set and for the last ProcessingStep
     * of a Process.
     */
    ProcessingStep next

    static mapping = {
        next index: 'processing_step_next_idx'
        previous index: 'processing_step_previous_idx'
        process index: 'processing_step_process_idx'
    }

    static constraints = {
        jobClass(nullable: true, blank: false)
        process(validator: { Process val, ProcessingStep obj ->
            if (val?.jobExecutionPlan?.id != obj.jobDefinition?.plan?.id) {
                return "jobExecutionPlan"
            }
            return true
        })
        previous(nullable: true, validator: { ProcessingStep val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            if (val.process?.id != obj.process?.id) {
                return "process"
            }
            if (val.jobDefinition?.id == obj.jobDefinition?.id) {
                return "jobDefinition"
            }
            if (val == obj.next) {
                return "next"
            }
            return true
        })
        next(nullable: true, validator: { ProcessingStep val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            if (val.process?.id != obj.process?.id) {
                return "process"
            }
            if (val.jobDefinition?.id == obj.jobDefinition?.id) {
                return "jobDefinition"
            }
            if (val == obj.previous) {
                return "previous"
            }
            return true
        })
        input(nullable: true, validator: { Collection<Parameter> val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            List<String> errors = []
            val.each { Parameter param ->
                if (param.type.jobDefinition.id != obj.jobDefinition.id) {
                    errors << "invalid.jobDefinition"
                }
                if (param.type.parameterUsage != ParameterUsage.INPUT) {
                    errors << "invalid.parameterUsage"
                }
                // check for uniqueness of parameter types
                for (Parameter p in obj.input) {
                    if (p == param) {
                        continue
                    }
                    if (p.type == param.type) {
                        errors << "unique.type"
                    }
                }
            }
            if (!errors) {
                return true
            }
            return errors
        })
        output(nullable: true, validator: { Collection<Parameter> val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            List<String> errors = []
            val.each { Parameter param ->
                if (param.type.jobDefinition.id != obj.jobDefinition.id) {
                    errors << "invalid.jobDefinition"
                }
                if (param.type.parameterUsage != ParameterUsage.OUTPUT && param.type.parameterUsage != ParameterUsage.PASSTHROUGH) {
                    errors << "invalid.parameterUsage"
                }
                // check for uniqueness of parameter types
                for (Parameter p in obj.output) {
                    if (p == param) {
                        continue
                    }
                    if (p.type == param.type) {
                        errors << "unique.type"
                    }
                }
            }
            if (!errors) {
                return true
            }
            return errors
        })
    }

    /**
     * @return The latest {@link ProcessingStepUpdate} belonging to this {@link ProcessingStep} or
     * <code>null</code> if this {@link ProcessingStep} has no {@link ProcessingStepUpdate}s.
     */
    ProcessingStepUpdate getLatestProcessingStepUpdate() {
        return ProcessingStepUpdate.findByProcessingStep(this, [sort: "id", order: "desc"])
    }

    /**
     * @return The first {@link ProcessingStepUpdate} belonging to this {@link ProcessingStep} or
     * <code>null</code> if this {@link ProcessingStep} has no {@link ProcessingStepUpdate}s.
     */
    ProcessingStepUpdate getFirstProcessingStepUpdate() {
        return ProcessingStepUpdate.findByProcessingStep(this, [sort: "id", order: "asc"])
    }

    /**
     * Convenience method to retrieve the non-qualified class name of the {@link Job} used for this {@link ProcessingStep}.
     *
     * @return the non-qualified class name, or <code>null</code> if {@link #jobClass} is <code>null</code>.
     */
    String getNonQualifiedJobClass() {
        jobClass?.split('\\.')?.last()
    }

    /**
     * Returns the object with the specified ID or throws an exception if no such object exists.
     */
    static ProcessingStep getInstance(final long id) {
        final ProcessingStep instance = ProcessingStep.get(id)
        if (instance == null) {
            throw new RuntimeException("No ProcessingStep with ID ${id} found in database.")
        }
        return instance
    }

    String getClusterJobName() {
        String env
        switch (Environment.current) {
            case Environment.PRODUCTION:
                env = 'prod'
                break
            case Environment.DEVELOPMENT:
                env = 'devel'
                break
            default:
                env = Environment.current.name.toLowerCase()
        }
        String psId  = this.id
        String psClass = this.getNonQualifiedJobClass()
        String psWorkflow = this.process.jobExecutionPlan
        String pid = this.getProcessParameterObject()?.individual?.pid
        return [
                'otp',
                env,
                pid,
                psWorkflow,
                psId,
                psClass,
        ].findAll().join('_')
    }

    // suppressing because this entity will be removed within the old workflow system
    @SuppressWarnings("ClassForName")
    boolean belongsToMultiJob() {
        Class jobClass = Class.forName(jobClass, true, getClass().getClassLoader())
        return AbstractMultiJob.isAssignableFrom(jobClass)
    }

    ProcessParameterObject getProcessParameterObject() {
        return process.processParameterObject
    }

    static ProcessingStep findTopMostProcessingStep(ProcessingStep step) {
        if (RestartedProcessingStep.isAssignableFrom(Hibernate.getClass(step))) {
            if (step.original) {
                return findTopMostProcessingStep(step.original)
            }
        }
        return step
    }

    JobExecutionPlan getJobExecutionPlan() {
        return process.jobExecutionPlan
    }
}
