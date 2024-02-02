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

import de.dkfz.tbi.TestCase

class IntegerFieldDefinitionSpec extends AbstractFieldDefinitionSpec {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                IntegerFieldDefinition,
        ]
    }

    @Override
    AbstractFieldDefinition createDefinition() {
        return createIntegerFieldDefinition()
    }

    void "test, when default value is in list of allowed value, then the validation pass"() {
        given:
        IntegerFieldDefinition definition = createIntegerFieldDefinition()

        when:
        definition.defaultIntegerValue = 3
        definition.allowedIntegerValues = [2, 3, 4]
        definition.validate()

        then:
        definition.errors.errorCount == 0
    }

    void "test, when default value is not in list of allowed value, then the validation fail"() {
        given:
        IntegerFieldDefinition definition = createIntegerFieldDefinition()

        when:
        definition.defaultIntegerValue = 3
        definition.allowedIntegerValues = [2, 4]

        then:
        TestCase.assertValidateError(definition, 'allowedIntegerValues', 'validator.defaultValue.not.in.allowedValues')
    }
}
