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

import de.dkfz.tbi.otp.config.TypeValidators

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class TextFieldDefinition extends AbstractFieldDefinition {

    TypeValidators typeValidator

    String regularExpression

    String regularExpressionError

    List<String> allowedTextValues

    String defaultTextValue

    static constraints = {
        typeValidator nullable: true, validator: { value, obj ->
            if (value && obj.defaultTextValue && !value.validate(obj.defaultValue)) {
                return ['validator.defaultValue.do.not.pass.validator', obj.defaultTextValue]
            }
        }
        regularExpression nullable: true, validator: { value, obj ->
            if (value) {
                if (!isValidPattern(value)) {
                    return 'validator.regularExpression.isNotValid'
                }
                if (obj.defaultTextValue && !(obj.defaultValue ==~ value)) {
                    return ['validator.defaultValue.do.not.pass.regExpression', obj.defaultTextValue]
                }
            }
        }
        regularExpressionError nullable: true
        defaultTextValue nullable: true
        allowedTextValues validator: { value, obj ->
            if (value && obj.defaultTextValue && !value.contains(obj.defaultTextValue)) {
                return ['validator.defaultValue.not.in.allowedValues', obj.defaultTextValue]
            }
        }
    }

    static mapping = {
        defaultTextValue type: 'text'
        regularExpression type: 'text'
        allowedTextValues lazy: false
    }

    @Override
    ProjectFieldType getProjectFieldType() {
        return ProjectFieldType.TEXT
    }

    @Override
    Object getDefaultValue() {
        return defaultTextValue
    }

    @Override
    List<Object> getValueList() {
        return allowedTextValues
    }

    static private boolean isValidPattern(String pattern) {
        try {
            Pattern.compile(pattern)
            return true
        } catch (PatternSyntaxException e) {
            return false
        }
    }
}
