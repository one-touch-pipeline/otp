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

import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.job.plan.*

/**
 * Service to test whether a given Job Execution Plan is valid.
 * This service just offers one method which validates a given Job Execution Plan
 * and returns all found errors as String values.
 *
 * Be aware that just because the validation does not return errors it does not
 * mean that the plan is correctly. Like always it's possible to find errors but
 * not possible to find all errors.
 *
 * The validation does not yet validate plans correctly containing DecisionJobs.
 */
@CompileDynamic
@Transactional
class PlanValidatorService {
    static final String NO_STARTJOB = "No StartJob defined for JobExecutionPlan"
    static final String STARTJOB_BEAN_MISSING = "The bean specified as a StartJob does not exist in the Spring context"
    static final String STARTJOB_BEAN_NOT_IMPLEMENTING_STARTJOB = "The bean specified as StartJob does not implement the StartJob interface"
    static final String NO_FIRSTJOB = "First JobDefinition not defined for JobExecutionPlan"
    static final String JOB_BEAN_MISSING = "The bean specified as Job does not exists in Spring context (JobDefinition, Bean name): "
    static final String JOB_BEAN_NOT_IMPLEMENTING_JOB = "The bean specified as a Job does not implement the Job interface (JobDefinition, Bean name): "
    static final String LAST_JOB_NOT_ENDSTATE_AWARE = "The last specified Job does not implement the end state aware interface"
    static final String CIRCULAR_JOBS = "The Job Execution Plan contains a circular Job dependency"
    static final String NOT_ALL_JOBS_LINKED = "Not all Job Definitions defined for this Job Execution Plan are linked through the next relationship"
    static final String VALIDATOR_LOOP = "Validator is specified before the job it should validate"
    static final String VALIDATOR_BEAN_NOT_IMPLEMENTING_INTERFACE = "The bean specified as a ValidatingJob does not implement the ValidatingJob interface" +
            " (JobDefinition, Bean name): "
    static final String VALIDATOR_ON_ENDSTATE = "The to be validated job is endstate aware (JobDefinition, Bean name): "

    /**
     * Dependency Injection of grailsApplication
     */
    def grailsApplication

    List<String> validate(JobExecutionPlan plan) {
        List<String> foundErrors = []
        if (hasStartJob(plan)) {
            // perform validation on StartJob
            if (startJobBeanExists(plan.startJob)) {
                if (!startJobBeanIsStartJob(plan.startJob)) {
                    foundErrors << STARTJOB_BEAN_NOT_IMPLEMENTING_STARTJOB
                }
            } else {
                foundErrors << STARTJOB_BEAN_MISSING
            }
        } else {
            foundErrors << NO_STARTJOB
        }

        if (!hasFirstJob(plan)) {
            foundErrors << NO_FIRSTJOB
        }
        List<JobDefinition> jobDefinitions = JobDefinition.findAllByPlan(plan, [sort: 'id', order: 'asc'])
        jobDefinitions.each { job ->
            if (job instanceof StartJobDefinition) {
                return
            }
            if (jobBeanExists(job)) {
                if (!jobBeanIsJob(job)) {
                    foundErrors << JOB_BEAN_NOT_IMPLEMENTING_JOB + "${job.id}, ${job.bean}"
                }
            } else {
                foundErrors << JOB_BEAN_MISSING + "${job.id}, ${job.bean}"
            }
            if (job instanceof ValidatingJobDefinition) {
                if (!validatingJobBeanIsValidatingJob(job as ValidatingJobDefinition)) {
                    foundErrors << VALIDATOR_BEAN_NOT_IMPLEMENTING_INTERFACE + "${job.id}, ${job.bean}"
                }
                if (!validatedJobBeforeValidator(plan, job as ValidatingJobDefinition)) {
                    foundErrors << VALIDATOR_LOOP
                }
                if (!validatedJobIsNotEndstateAware(job as ValidatingJobDefinition)) {
                    foundErrors << VALIDATOR_ON_ENDSTATE + "${job.id}, ${job.bean}"
                }
            }
        }
        if (noCircularDependency(plan)) {
            // checks which may not be in a circular dependency
            if (!lastJobIsEndStateAware(plan)) {
                foundErrors << LAST_JOB_NOT_ENDSTATE_AWARE
            }
            if (!allJobsAreLinked(plan)) {
                foundErrors << NOT_ALL_JOBS_LINKED
            }
        } else {
            foundErrors << CIRCULAR_JOBS
        }
        return foundErrors
    }

    private boolean hasStartJob(JobExecutionPlan plan) {
        return plan.startJob
    }

    private boolean startJobBeanExists(StartJobDefinition startJob) {
        if (!startJob) {
            return false
        }
        return Holders.grailsApplication.mainContext.containsBean(startJob.bean)
    }

    private boolean startJobBeanIsStartJob(StartJobDefinition startJob) {
        if (!startJob) {
            return false
        }
        def startJobBean = Holders.grailsApplication.mainContext.getBean(startJob.bean)
        return (startJobBean instanceof StartJob)
    }

    private boolean hasFirstJob(JobExecutionPlan plan) {
        return plan.firstJob
    }

    private boolean jobBeanExists(JobDefinition job) {
        return Holders.grailsApplication.mainContext.containsBean(job.bean)
    }

    private boolean jobBeanIsJob(JobDefinition job) {
        def jobBean = Holders.grailsApplication.mainContext.getBean(job.bean)
        return (jobBean instanceof Job)
    }

    private boolean noCircularDependency(JobExecutionPlan plan) {
        if (!plan.firstJob) {
            return true
        }
        List<JobDefinition> processedJobs = []
        JobDefinition job = plan.firstJob
        while (job.next) {
            processedJobs << job
            job = job.next
            if (processedJobs.contains(job)) {
                return false
            }
        }
        return true
    }

    private boolean lastJobIsEndStateAware(JobExecutionPlan plan) {
        if (!plan.firstJob) {
            return false
        }
        JobDefinition lastJob = plan.firstJob
        while (lastJob.next) {
            lastJob = lastJob.next
        }
        if (jobBeanExists(lastJob)) {
            def jobBean = Holders.grailsApplication.mainContext.getBean(lastJob.bean)
            return (jobBean instanceof EndStateAwareJob)
        }
        return false
    }

    private boolean allJobsAreLinked(JobExecutionPlan plan) {
        if (!plan.firstJob) {
            return true
        }
        List<Long> jobDefinitions = JobDefinition.findAllByPlan(plan).findAll { !(it instanceof StartJobDefinition) }*.id
        List<Long> linkedDefinition = []
        JobDefinition lastJob = plan.firstJob
        while (lastJob) {
            linkedDefinition << lastJob.id
            lastJob = lastJob.next
        }
        for (Long id in jobDefinitions) {
            if (!linkedDefinition.contains(id)) {
                return false
            }
        }
        return true
    }

    private boolean validatingJobBeanIsValidatingJob(ValidatingJobDefinition validator) {
        def jobBean = Holders.grailsApplication.mainContext.getBean(validator.bean)
        return (jobBean instanceof ValidatingJob)
    }

    private boolean validatedJobBeforeValidator(JobExecutionPlan plan, ValidatingJobDefinition validator) {
        JobDefinition jobDefinition = plan.firstJob
        while (jobDefinition.next) {
            if (jobDefinition == validator) {
                return false
            }
            if (jobDefinition == validator.validatorFor) {
                return true
            }
            jobDefinition = jobDefinition.next
        }
        return false
    }

    private boolean validatedJobIsNotEndstateAware(ValidatingJobDefinition validator) {
        def jobBean = Holders.grailsApplication.mainContext.getBean(validator.validatorFor.bean)
        return !(jobBean instanceof EndStateAwareJob)
    }
}
