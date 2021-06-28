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

class TextFieldValue extends AbstractSingleFieldValue {

    String textValue

    @SuppressWarnings("Instanceof")
    static constraints = {
        textValue nullable: true, validator: { val, obj ->
            if (val && obj.definition instanceof TextFieldDefinition) {
                if (obj.definition.allowedTextValues && !(val in obj.definition.allowedTextValues)) {
                    return ["textFieldValue.textValue.notInList", obj.definition.allowedTextValues, obj.definition.name]
                }
                if (obj.definition.typeValidator && !obj.definition.typeValidator.validate(val)) {
                    return ["textFieldValue.textValue.wrongType", obj.definition.typeValidator, obj.definition.name]
                }
                if (obj.definition.regularExpression && !(val ==~ obj.definition.regularExpression)) {
                    return ["textFieldValue.textValue.regex", obj.definition.regularExpression, obj.definition.name, obj.definition.regularExpressionError]
                }
                return true
            }
        }
    }

    static mapping = {
        textValue type: 'text'
    }

    @Override
    ProjectFieldType getProjectFieldType() {
        return ProjectFieldType.TEXT
    }

    @Override
    String getDisplayValue() {
        return textValue ?: ""
    }
}
