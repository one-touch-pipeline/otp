/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.project.additionalField

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateFieldDefinition extends AbstractFieldDefinition {

    List<LocalDate> allowedDateValues

    LocalDate defaultDateValue

    static constraints = {
        defaultDateValue nullable: true
        allowedDateValues validator: { value, obj ->
            if (value && obj.defaultDateValue && !value.contains(obj.defaultDateValue)) {
                return ['validator.defaultValue.not.in.allowedValues', obj.defaultDateValue]
            }
        }
    }

    static mapping = {
        allowedDateValues lazy: false
    }

    @Override
    ProjectFieldType getProjectFieldType() {
        return ProjectFieldType.DATE
    }

    @Override
    Object getDefaultValue() {
        return defaultDateValue
    }

    @Override
    List<Object> getValueList() {
        return allowedDateValues*.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
