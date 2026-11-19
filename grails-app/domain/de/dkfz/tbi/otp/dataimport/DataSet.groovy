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

/**
 * A registered data set: one row of an import request of a {@link DataTypeVersion}.
 */
@ManagedEntity
@ToString(includeNames = true, includePackage = false)
class DataSet implements Entity {

    /** the ticket of the import request, the reference for later inquiries */
    Ticket ticket

    Project project

    DataTypeVersion dataTypeVersion

    /** the row of the import request as JSON object */
    String rawJsonData

    static Closure constraints = {
        rawJsonData validator: { String val, DataSet obj, Errors errors ->
            JsonValidationError.rejectAll(DataSetValidator.validateRawJsonData(val, obj.dataTypeVersion), errors, 'dataSet', 'rawJsonData', val)
        }
    }

    static Closure mapping = {
        rawJsonData type: 'text'
        ticket index: 'data_set_ticket_idx'
        project index: 'data_set_project_idx'
        dataTypeVersion index: 'data_set_data_type_version_idx'
    }
}
