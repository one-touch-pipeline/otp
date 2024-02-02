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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

class MateNumberValidatorSpec extends Specification {

    void 'validate, when column is missing, adds an error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.ERROR, "Required column 'READ' is missing.",
                        "Required column 'READ' is missing.")
        ]

        when:
        new MateNumberValidator().validate(context)

        then:
        context.problems.first().message == expectedProblems.first().message
    }

    @Unroll
    void 'validate, when value is "#value", then validation should not return an error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.READ}
${value}
""")

        when:
        new MateNumberValidator().validate(context)

        then:
        context.problems.empty

        where:
        value << [
                '1',
                '2',
                '3',
                'i1',
                'I1',
                'i2',
                'I2',
                'i3',
                'I3',
        ]
    }

    @Unroll
    void 'validate, when value is "#value", then adds expected errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.READ}
${value}
""")

        when:
        new MateNumberValidator().validate(context)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR, message, summaryMessage),
        ]

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)

        where:
        value  || message                                                                 | summaryMessage
        ''     || MateNumberValidator.ERROR_NOT_PROVIDED                                  | MateNumberValidator.ERROR_NOT_PROVIDED
        '-1'   || "The mate number ('-1') ${MateNumberValidator.ALLOWED_VALUE_POSTFIX}"   | MateNumberValidator.ERROR_INVALID_VALUE_SUMMARY
        '0'    || "The mate number ('0') ${MateNumberValidator.ALLOWED_VALUE_POSTFIX}"    | MateNumberValidator.ERROR_INVALID_VALUE_SUMMARY
        'abc'  || "The mate number ('abc') ${MateNumberValidator.ALLOWED_VALUE_POSTFIX}"  | MateNumberValidator.ERROR_INVALID_VALUE_SUMMARY
        'i'    || "The mate number ('i') ${MateNumberValidator.ALLOWED_VALUE_POSTFIX}"    | MateNumberValidator.ERROR_INVALID_VALUE_SUMMARY
        'I'    || "The mate number ('I') ${MateNumberValidator.ALLOWED_VALUE_POSTFIX}"    | MateNumberValidator.ERROR_INVALID_VALUE_SUMMARY
        '123i' || "The mate number ('123i') ${MateNumberValidator.ALLOWED_VALUE_POSTFIX}" | MateNumberValidator.ERROR_INVALID_VALUE_SUMMARY
    }
}
