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
package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.project.ProjectFieldReferenceAble
import de.dkfz.tbi.otp.project.additionalField.*

import java.time.LocalDate

trait ProjectFieldsDomainFactory implements DomainFactoryHelper {

    private Map<String, Object> createBaseDefinitions() {
        return [
                name                             : "name_${nextId}",
                descriptionConfig                : "config description ${nextId}",
                descriptionRequest               : "request description ${nextId}",
                fieldUseForSequencingProjects    : FieldExistenceType.REQUIRED,
                fieldUseForDataManagementProjects: FieldExistenceType.REQUIRED,
                sourceOfData                     : ProjectSourceOfData.REQUESTER,
                projectDisplayOnConfigPage       : ProjectDisplayOnConfigPage.SHOW,
                sortNumber                       : nextId,
                cardinalityType                  : ProjectCardinalityType.SINGLE,
        ]
    }

    TextFieldDefinition createTextFieldDefinition(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(TextFieldDefinition, createBaseDefinitions(), properties, saveAndValidate)
    }

    IntegerFieldDefinition createIntegerFieldDefinition(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(IntegerFieldDefinition, createBaseDefinitions(), properties, saveAndValidate)
    }

    DecimalNumberFieldDefinition createDecimalFieldDefinition(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DecimalNumberFieldDefinition, createBaseDefinitions(), properties, saveAndValidate)
    }

    FlagFieldDefinition createFlagFieldDefinition(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(FlagFieldDefinition, createBaseDefinitions(), properties, saveAndValidate)
    }

    DateFieldDefinition createDateFieldDefinition(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DateFieldDefinition, createBaseDefinitions(), properties, saveAndValidate)
    }

    DomainReferenceFieldDefinition createDomainReferenceFieldDefinition(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DomainReferenceFieldDefinition, createBaseDefinitions() + [
                domainClassName: "${ProjectFieldReferenceAble.name}",
        ], properties, saveAndValidate)
    }

    TextFieldValue createTextFieldValue(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(TextFieldValue, [
                definition: { createTextFieldDefinition() },
                textValue : "someText ${nextId}",
        ], properties, saveAndValidate)
    }

    IntegerFieldValue createIntegerFieldValue(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(IntegerFieldValue, [
                definition  : { createIntegerFieldDefinition() },
                integerValue: nextId,
        ], properties, saveAndValidate)
    }

    DecimalNumberFieldValue createDecimalFieldValue(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DecimalNumberFieldValue, [
                definition        : { createDecimalFieldDefinition() },
                decimalNumberValue: { nextId * 1.01 + 0.001 },
        ], properties, saveAndValidate)
    }

    FlagFieldValue createFlagFieldValue(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(FlagFieldValue, [
                definition: { createFlagFieldDefinition() },
                flagValue : true,
        ], properties, saveAndValidate)
    }

    DateFieldValue createDateFieldValue(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DateFieldValue, [
                definition: { createDateFieldDefinition() },
                dateValue : { LocalDate.now() },
        ], properties, saveAndValidate)
    }

    DomainReferenceFieldValue createDomainReferenceFieldValue(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(DomainReferenceFieldValue, [
                definition                      : { createDomainReferenceFieldDefinition() },
                domainId                        : nextId,
                cachedTextRepresentationOfDomain: "cachedRepresentation ${nextId}",
        ], properties, saveAndValidate)
    }

    SetValueField createSetValueField(Map properties = [:], boolean saveAndValidate = true) {
        AbstractFieldDefinition definition = properties.definition ?:
                properties.values ? properties.values.first().definition : createTextFieldDefinition()
        return createDomainObject(SetValueField, [
                definition: definition,
        ], properties, saveAndValidate)
    }
}
