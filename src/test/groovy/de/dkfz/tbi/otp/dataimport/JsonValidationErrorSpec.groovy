/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataimport

import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import spock.lang.Specification

class JsonValidationErrorSpec extends Specification {

    static class Target {
        String json
    }

    void "rejectAll, when there are several errors, then add one field error for each with the codes and arguments as Grails does"() {
        given:
        Errors errors = new BeanPropertyBindingResult(new Target(json: "x"), "target")
        List<JsonValidationError> jsonErrors = [
                new JsonValidationError("code.a", "#/a", "detail a"),
                new JsonValidationError("code.b", "line 1, column 2", null),
        ]

        when:
        JsonValidationError.rejectAll(jsonErrors, errors, "target", "json", "x")

        then:
        List<FieldError> fieldErrors = errors.getFieldErrors("json")
        fieldErrors.size() == 2
        fieldErrors*.code == ["code.a", "code.b"]
        fieldErrors*.codes*.toList() == [["target.json.code.a", "code.a"], ["target.json.code.b", "code.b"]]
        fieldErrors*.arguments*.toList() == [["#/a", "detail a"], ["line 1, column 2", null]]
        fieldErrors*.defaultMessage == ["code.a at #/a: detail a", "code.b at line 1, column 2"]
        fieldErrors*.rejectedValue == ["x", "x"]
    }

    void "rejectAll, when there are no errors, then add nothing"() {
        given:
        Errors errors = new BeanPropertyBindingResult(new Target(), "target")

        when:
        JsonValidationError.rejectAll([], errors, "target", "json", null)

        then:
        !errors.hasErrors()
    }
}
