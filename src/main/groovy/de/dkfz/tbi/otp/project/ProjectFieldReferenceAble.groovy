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
package de.dkfz.tbi.otp.project

import de.dkfz.tbi.otp.project.additionalField.DomainReferenceFieldDefinition
import de.dkfz.tbi.otp.project.additionalField.ProjectFieldType

/**
 * Marker interface for domains which should be referencable by {@link DomainReferenceFieldDefinition}.
 *
 * Only classes of grails artefact type entity implementing this interface are available for additional attributes.
 * Since the reference to the class are saved in the database, moving, renaming and removing of the class require database migration.
 */
interface ProjectFieldReferenceAble {

    /**
     * Defines how the domain should be displayed on views using FieldValues of the {@link ProjectFieldType#DOMAIN_REFERENCE}
     */
    String getStringForProjectFieldDomainReference()
}
