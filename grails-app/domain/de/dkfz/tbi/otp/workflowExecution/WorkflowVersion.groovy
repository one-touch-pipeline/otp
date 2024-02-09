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
class WorkflowVersion implements Entity, Commentable {

    WorkflowApiVersion apiVersion
    String workflowVersion
    LocalDate deprecatedDate
    Set<ReferenceGenome> allowedReferenceGenomes
    Set<SeqType> supportedSeqTypes

    String getDisplayName() {
        return "${workflow.name} ${workflowVersion}"
    }

    static Closure constraints = {
        deprecatedDate nullable: true
        comment nullable: true
        apiVersion unique: 'workflowVersion'
        workflowVersion validator: { String val, WorkflowVersion obj ->
            WorkflowVersion existingWorkflowVersion = createCriteria().get {
                ne('id', obj.id)
                'apiVersion' {
                    eq('workflow', obj.workflow)
                }
                eq('workflowVersion', val)
            } as WorkflowVersion
            if (existingWorkflowVersion) {
                obj.errors.rejectValue('workflowVersion', 'invalid.workflowVersion', 'Workflow Version already exists for workflow.')
            }
        }
    }

    static Closure mapping = {
        apiVersion index: "workflow_version_api_version_idx"
    }

    static hasMany = [
            allowedReferenceGenomes: ReferenceGenome,
            supportedSeqTypes      : SeqType,
    ]

    static belongsTo = [
            apiVersion: WorkflowApiVersion,
    ]

    @Override
    String toString() {
        return displayName
    }

    Workflow getWorkflow() {
        return apiVersion.workflow
    }
}
