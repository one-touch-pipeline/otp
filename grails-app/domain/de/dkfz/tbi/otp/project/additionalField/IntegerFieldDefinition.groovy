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
package de.dkfz.tbi.otp.project.additionalField

import grails.gorm.hibernate.annotation.ManagedEntity

@ManagedEntity
class IntegerFieldDefinition extends AbstractFieldDefinition {

    List<Integer> allowedIntegerValues

    Integer defaultIntegerValue

    static constraints = {
        defaultIntegerValue nullable: true
        allowedIntegerValues validator: { value, obj ->
            if (value && obj.defaultIntegerValue && !value.contains(obj.defaultIntegerValue)) {
                return ['validator.defaultValue.not.in.allowedValues', obj.defaultIntegerValue]
            }
        }
    }

    static mapping = {
        allowedIntegerValues lazy: false
    }

    @Override
    ProjectFieldType getProjectFieldType() {
        return ProjectFieldType.INTEGER
    }

    @Override
    Object getDefaultValue() {
        return defaultIntegerValue
    }

    @Override
    List<Object> getValueList() {
        return allowedIntegerValues
    }
}
