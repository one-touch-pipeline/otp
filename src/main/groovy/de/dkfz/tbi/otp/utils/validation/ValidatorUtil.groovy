/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.utils.validation

import groovy.transform.TupleConstructor
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.springframework.validation.Errors

class ValidatorUtil {
    /**
     * This method simplifies the usage of Errors.rejectValue() and Errors.reject() in validators
     * by providing the methods rejectValue() and reject(),
     * and makes it possible to use the same message codes as when returning strings from the validator.
     */
    static <T, U>  Closure messageArgs(String propertyName, @ClosureParams(value = FromString, options = "T, U, Closure") Closure validator) {
        return { T val, U obj, Errors errors ->
            validator.delegate = new ValidatorDelegate(
                    rejectValue.curry(propertyName, obj, val, errors),
                    reject.curry(obj, errors),
                    errors,
            )
            return validator(val as T, obj as U)
        }
    }

    @TupleConstructor
    static class ValidatorDelegate {
        Closure rejectValue
        Closure reject
        Errors errors
    }

    static Closure rejectValue = { String propertyName, Object obj, Object val, Errors errors, String message, List args = [] ->
        errors.rejectValue(propertyName, "${obj.class.simpleName.uncapitalize()}.${propertyName}.${message}", ([propertyName, obj.class.simpleName, val] + args) as Object[], "Field '${propertyName}' with value '${val}' does not pass custom validation")
    }

    static Closure reject = { Object obj, Errors errors, String message, List args = [] ->
        errors.reject("${obj.class.simpleName.uncapitalize()}.${message}", ([obj.class.simpleName] + args) as Object[], "Fields do not pass custom validation")
    }
}
