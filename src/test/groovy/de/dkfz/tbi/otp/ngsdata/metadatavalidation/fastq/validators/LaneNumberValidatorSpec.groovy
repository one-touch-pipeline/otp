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

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame

class LaneNumberValidatorSpec extends Specification {

    void 'validate adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.LANE_NO}\n" +
                        "0\n" +
                        "1\n" +
                        "8\n" +
                        "9\n" +
                        "001\n" +
                        "1a\n" +
                        "\n" +
                        "1_ABC")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.WARNING,
                        "'0' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, LogLevel.WARNING,
                        "'9' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, LogLevel.WARNING,
                        "'001' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, LogLevel.WARNING,
                        "'1a' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, LogLevel.ERROR,
                        "The lane number must not be empty."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, LogLevel.ERROR,
                        "'1_ABC' is not a well-formed lane number. It must contain only digits (0 to 9) and/or letters (a to z, A to Z). It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
        ]

        when:
        new LaneNumberValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
