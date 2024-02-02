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

import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.TypeValidators

class TextFieldDefinitionSpec extends AbstractFieldDefinitionSpec {

    static private final String DEFAULT_VALUE = 'DEFAULT VALUE'

    @Override
    Class[] getDomainClassesToMock() {
        return [
                TextFieldDefinition,
        ]
    }

    @Override
    AbstractFieldDefinition createDefinition() {
        return createTextFieldDefinition()
    }

    @Unroll
    void "test, when default value is valid for constraint #property, then the validation pass"() {
        given:
        TextFieldDefinition definition = createTextFieldDefinition()

        when:
        definition.defaultTextValue = DEFAULT_VALUE
        definition[property] = value
        definition.validate()

        then:
        definition.errors.errorCount == 0

        where:
        property            | value
        'allowedTextValues' | ['test1', DEFAULT_VALUE, 'test2']
        'typeValidator'     | TypeValidators.SINGLE_LINE_TEXT
        'regularExpression' | '[A-Z ]*'
    }

    @Unroll
    void "test, when default value is not valid for constraint #property, then the validation fail"() {
        given:
        TextFieldDefinition definition = createTextFieldDefinition()

        when:
        definition.defaultTextValue = DEFAULT_VALUE
        definition[property] = value

        then:
        TestCase.assertValidateError(definition, property, constraint)

        where:
        property            | value                           | constraint
        'allowedTextValues' | ['test1', 'test2']              | 'validator.defaultValue.not.in.allowedValues'
        'typeValidator'     | TypeValidators.SINGLE_WORD_TEXT | 'validator.defaultValue.do.not.pass.validator'
        'regularExpression' | '[A-Z]*'                        | 'validator.defaultValue.do.not.pass.regExpression'
    }

    void "test, when regular expression is not valid pattern, then the validation fail"() {
        given:
        TextFieldDefinition definition = createTextFieldDefinition()

        when:
        definition.regularExpression = '[abc['

        then:
        TestCase.assertValidateError(definition, 'regularExpression', 'validator.regularExpression.isNotValid')
    }
}
