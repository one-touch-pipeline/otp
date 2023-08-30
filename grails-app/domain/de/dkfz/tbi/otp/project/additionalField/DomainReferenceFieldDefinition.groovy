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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.project.ProjectFieldReferenceAble

@ManagedEntity
class DomainReferenceFieldDefinition extends AbstractFieldDefinition {

    String domainClassName

    Long defaultDomainReferenceId

    /**
     * Flag to indicate, if the customer should be allowed to provide their own value.
     */
    boolean allowCustomValue

    static constraints = {
        domainClassName blank: false, validator: { value ->
            if (!value) {
                return
            }
            Class clazz
            try {
                clazz = DomainReferenceFieldValue.classLoader.loadClass(value)
            } catch (ClassNotFoundException e) {
                return 'validator.class.not.found'
            }
            return ProjectFieldReferenceAble.isAssignableFrom(clazz) ? true :
                    'domainReferenceFieldDefinition.class.not.assignable.from.ProjectFieldReferenceAble'
        }
        defaultDomainReferenceId nullable: true
    }

    @Override
    ProjectFieldType getProjectFieldType() {
        return ProjectFieldType.DOMAIN_REFERENCE
    }

    @Override
    Object getDefaultValue() {
        return DomainReferenceFieldValue.classLoader.loadClass(domainClassName).get(defaultDomainReferenceId)
    }

    @Override
    List<Object> getValueList() {
        return []
    }

    String shortClassName() {
        return domainClassName.substring(domainClassName.lastIndexOf('.') + 1)
    }
}
