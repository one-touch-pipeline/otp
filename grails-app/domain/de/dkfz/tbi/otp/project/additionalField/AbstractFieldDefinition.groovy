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

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.Legacy

abstract class AbstractFieldDefinition implements Entity, Legacy {

    String name

    String descriptionConfig

    String descriptionRequest

    FieldExistenceType fieldUseForSequencingProjects

    FieldExistenceType fieldUseForDataManagementProjects

    ProjectSourceOfData sourceOfData

    ProjectDisplayOnConfigPage projectDisplayOnConfigPage

    /**
     * Number for sorting fields to display
     */
    int sortNumber

    ProjectCardinalityType cardinalityType

    /**
     * Flag to indicate, that this field may only be changed by an operator.
     * Normal fields may also be changed by project members.
     */
    boolean changeOnlyByOperator = false

    /**
     * A flag to indicate, that the value is used in external tools and
     * therefore the name shouldn't be changed without coordinating the change with all external dependencies
     */
    boolean usedExternally = false

    abstract ProjectFieldType getProjectFieldType()

    abstract Object getDefaultValue()

    abstract List<Object> getValueList()

    static constraints = {
        name unique: true, blank: false
        descriptionConfig blank: false
        descriptionRequest blank: false
    }

    static mapping = {
        descriptionConfig type: 'text'
        descriptionRequest type: 'text'
    }
}
