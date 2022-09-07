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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * The Process represents one execution of a {@link JobExecutionPlan}.
 *
 * A Process is triggered by a {@link StartJob} and generated from a {@link JobExecutionPlan}. It
 * groups all the {@link Job}s run for the various {@link JobDefinition}s. For each run of a {@link Job} a
 * {@link ProcessingStep} is added to this Process.
 *
 * For each JobExecutionPlan there can be many Processes representing the various executions for
 * different values (e.g. different file name).
 *
 * @see ProcessingStep
 * @see JobExecutionPlan
 * @see JobDefinition
 * @see Job
 * @see StartJob
 *
 * @deprecated class is part of the old workflow system
 */
@Deprecated
@ManagedEntity
class Process implements CommentableWithProject, Serializable, Entity {
    /**
     * The Date when this process was started.
     */
    Date started
    /**
     * The name of the StartJob class which triggered this Process.
     */
    String startJobClass
    /**
     * The JobExecutionPlan this Process belongs to
     */
    JobExecutionPlan jobExecutionPlan
    /**
     * Whether there are Jobs still running (false) for this process or not (true).
     */
    boolean finished = false

    boolean operatorIsAwareOfFailure = false

    /**
     * The value is set if this Process has been created in response to the failure of the referenced Process.
     */
    Process restarted

    static Closure mapping = {
        finished index: 'finished_idx'
        jobExecutionPlan index: 'job_execution_plan_idx'
        started index: 'process_started_idx'
        comment cascade: "all-delete-orphan"
    }

    static constraints = {
        jobExecutionPlan(nullable: false)
        started(nullable: false)
        startJobClass(nullable: false, blank: false)
        restarted(nullable: true, unique: true)
        comment(nullable: true)
    }

    ProcessParameterObject getProcessParameterObject() {
        return atMostOneElement(ProcessParameter.findAllByProcess(this))?.toObject()
    }

    @Override
    Project getProject() {
        return processParameterObject?.project
    }
}
