/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataimport

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.ToString
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

/**
 * A received import request of a {@link DataTypeVersion}, waiting to be registered as {@link DataSet}s.
 */
@ManagedEntity
@ToString(includeNames = true, includePackage = false, excludes = ['rawJsonData'])
class RegisterDataSet implements Entity {

    Ticket ticket

    DataTypeVersion dataTypeVersion

    ProcessingPriority priority

    /** the projects referenced by the import request */
    Set<Project> projects

    /** the import request as JSON, only checked to be valid JSON */
    String rawJsonData

    WorkflowCreateState state = WorkflowCreateState.WAITING

    static hasMany = [
            projects: Project,
    ]

    static Closure constraints = {
        projects nullable: false, minSize: 1
        rawJsonData validator: { String val, RegisterDataSet obj, Errors errors ->
            JsonValidationError.rejectAll(DataImportSchemaValidator.validateJson(val), errors, 'registerDataSet', 'rawJsonData', val)
        }
    }

    static Closure mapping = {
        rawJsonData type: 'text'
        ticket index: 'register_data_set_ticket_idx'
        dataTypeVersion index: 'register_data_set_data_type_version_idx'
        priority index: 'register_data_set_priority_idx'
        state index: 'register_data_set_state_idx'
    }
}
