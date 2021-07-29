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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.TypeValidators

class TextFieldValueSpec extends AbstractSingleFieldValueSpec {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                TextFieldDefinition,
                TextFieldValue,
        ]
    }

    @Override
    AbstractFieldValue createValue() {
        return createTextFieldValue()
    }

    void "test constraint, allowed text values validation fails"() {
        given:
        AbstractFieldValue fieldValue = createValue()
        fieldValue.definition.allowedTextValues = ["asdf", "xyz", "abc"]

        when:
        fieldValue.textValue = 'invalidValue'

        then:
        TestCase.assertValidateError(fieldValue, "textValue", 'textFieldValue.textValue.notInList', 'invalidValue')
    }

    void "test constraint, type validation fails"() {
        given:
        AbstractFieldValue fieldValue = createValue()
        fieldValue.definition.typeValidator = TypeValidators.BOOLEAN

        when:
        fieldValue.textValue = 'invalidValue'

        then:
        TestCase.assertValidateError(fieldValue, "textValue", 'textFieldValue.textValue.wrongType', 'invalidValue')
    }

    void "test constraint, regular expression validation fails"() {
        given:
        AbstractFieldValue fieldValue = createValue()
        fieldValue.definition.regularExpression = "asdf"

        when:
        fieldValue.textValue = 'invalidValue'

        then:
        TestCase.assertValidateError(fieldValue, "textValue", 'textFieldValue.textValue.regex', 'invalidValue')
    }

    void "test constraint, validation succeeds"() {
        given:
        AbstractFieldValue fieldValue = createValue()
        fieldValue.definition.allowedTextValues = ["true", "untrue"]
        fieldValue.definition.typeValidator = TypeValidators.BOOLEAN
        fieldValue.definition.regularExpression = /^true|unknown$/

        when:
        fieldValue.textValue = 'true'

        then:
        fieldValue.validate()
    }
}
