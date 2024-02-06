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
import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Deprecateable
import de.dkfz.tbi.otp.utils.Entity

@ToString(includeNames = true, includePackage = false)
@ManagedEntity
class WorkflowVersionSelector implements Deprecateable<WorkflowVersionSelector>, Entity {

    Project project
    SeqType seqType

    WorkflowVersion workflowVersion
    WorkflowVersionSelector previous

    static Closure constraints = {
        previous nullable: true
        deprecationDate nullable: true
        seqType nullable: true
    }

    static Closure mapping = {
        project index: "workflow_version_selector_project_idx"
        seqType index: "workflow_version_selector_seq_type_idx"
        workflowVersion index: "workflow_version_selector_workflow_version_idx"
        previous index: "workflow_version_selector_previous_idx"
    }

    @Override
    String toString() {
        return "WVS ${deprecationDate ? "[DEPRECATED]" : ""}: (${project} ${seqType}) -> (${workflowVersion})"
    }
}
