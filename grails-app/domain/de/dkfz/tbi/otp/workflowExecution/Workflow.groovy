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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.Entity

import java.time.LocalDate

@ManagedEntity
class Workflow implements Commentable, Entity {

    String name

    String beanName

    boolean enabled

    /**
     * Priority relative to other Workflows.
     *
     * This priority is considered during scheduling to determine which type of Workflow to start first.
     */
    short priority = 0

    /**
     * limit of the number of parallel runs for a workflow
     */
    short maxParallelWorkflows

    LocalDate deprecatedDate

    /**
     * Default allowedReferenceGenomes that are defined on new WorkflowVersions
     */
    Set<ReferenceGenome> defaultReferenceGenomesForWorkflowVersions

    /**
     * Default supportedSeqTypes that are defined on new WorkflowVersions
     */
    Set<SeqType> defaultSeqTypesForWorkflowVersions

    WorkflowVersion defaultVersion

    static Closure constraints = {
        name unique: true
        beanName nullable: true
        deprecatedDate nullable: true
        comment nullable: true
        defaultVersion nullable: true, validator: { val, obj ->
            if (val) {
                return val.workflow == obj
            }
        }
    }

    static Closure mapping = {
        comment cascade: "all-delete-orphan"
    }

    String getDisplayName() {
        return "${name}${deprecatedDate ? " (deprecated)" : ""}"
    }

    @Override
    String toString() {
        return displayName
    }

    static hasMany = [
            defaultReferenceGenomesForWorkflowVersions: ReferenceGenome,
            defaultSeqTypesForWorkflowVersions        : SeqType,
    ]
}
