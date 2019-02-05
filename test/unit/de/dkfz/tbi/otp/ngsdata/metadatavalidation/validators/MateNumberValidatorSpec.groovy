/*
 * Copyright 2011-2019 The OTP authors
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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

class MateNumberValidatorSpec extends Specification {

    void 'validate, when column is missing, adds no error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        when:
        new MateNumberValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, all are fine'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MATE}
1
2
""")

        when:
        new MateNumberValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MATE}

-1
0
1
2
3
4
abc
""")

        when:
        new MateNumberValidator().validate(context)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The mate number must be provided and must be a positive integer (value >= 1)."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The mate number ('-1') must be a positive integer (value >= 1).", "At least one mate number is not a positive integer number."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The mate number ('0') must be a positive integer (value >= 1).", "At least one mate number is not a positive integer number."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The mate number ('abc') must be a positive integer (value >= 1).", "At least one mate number is not a positive integer number."),
        ]

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
