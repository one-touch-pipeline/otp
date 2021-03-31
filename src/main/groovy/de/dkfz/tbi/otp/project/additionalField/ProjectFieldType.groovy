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

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.project.*

/**
 * Defines the possible data types for {@link ProjectPropertiesGivenWithRequest}.
 *
 * For each type a definition class {@link AbstractFieldDefinition} and a value class {@link AbstractFieldValue} is required.
 */
@TupleConstructor
enum ProjectFieldType {

    /**
     * The field contains some text
     */
    TEXT(TextFieldDefinition, TextFieldValue, ProjectFieldsCreateTextCommand,
            'textArea', 'defaultTextValue', 'multiText', 'allowedTextValues'),

    /**
     * The field is boolean and can contain only true and false
     */
    FLAG(FlagFieldDefinition, FlagFieldValue, ProjectFieldsCreateFlagCommand,
            'toggle', 'defaultFlagValue'),

    /**
     * The field is a number without fraction
     */
    INTEGER(IntegerFieldDefinition, IntegerFieldValue, ProjectFieldsCreateIntegerCommand,
            'integer', 'defaultIntegerValue', 'multiInteger', 'allowedIntegerValues'),

    /**
     * The field is a number with fraction
     */
    DECIMAL_NUMBER(DecimalNumberFieldDefinition, DecimalNumberFieldValue, ProjectFieldsCreateDecimalNumbersCommand,
            'default', 'defaultDecimalNumberValue', 'multiText', 'allowedDecimalNumberValues'),

    /**
     * The field represent a date
     */
    DATE(DateFieldDefinition, DateFieldValue, ProjectFieldsCreateDateCommand,
            'date', 'defaultDateValue', 'multiDate', 'allowedDateValues'),

    /**
     * The field represent a domain reference.
     */
    DOMAIN_REFERENCE(DomainReferenceFieldDefinition, DomainReferenceFieldValue, ProjectFieldsCreateDomainReferenceCommand,
            'select', 'defaultDomainReferenceId'),

    /**
     * Reference the {@link AbstractFieldDefinition} to define this type of field
     */
    final Class<? extends AbstractFieldDefinition> definitionClass

    /**
     * Reference the {@link AbstractFieldValue} to hold a value of this type of field
     */
    final Class<? extends AbstractSingleFieldValue> valueClass
    /**
     * Reference the {@link AbstractFieldValue} to hold a value of this type of field
     */
    final Class<? extends ProjectFieldsCreateCommand> commandClass

    final String templateDefaultValue
    final String propertyDefaultValue

    final String templateAllowedValue
    final String propertyAllowedValue

    String createActionName() {
        return 'create' + name().split('_').collect {
            "${it[0]}${it[1..-1].toLowerCase()}"
        }.join('')
    }

    /**
     * Returns only the values currently supported: Text and Integer
     * and all others are ignored (until they are implemented)
     * This method replaces the build-in values() for GSP page
     */
    static ProjectFieldType[] getSupportedValues() {
        return [ProjectFieldType.TEXT, ProjectFieldType.INTEGER]
    }
}
