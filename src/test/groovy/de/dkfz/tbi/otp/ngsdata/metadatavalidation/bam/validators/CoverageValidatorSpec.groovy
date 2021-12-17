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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.COVERAGE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class CoverageValidatorSpec extends Specification {

    void 'validate, when column COVERAGE missing, then add expected problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext()
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.WARNING,
                        "Optional column '${COVERAGE}' is missing.")
        ]

        when:
        new CoverageValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate context with errors'() {
        given:
        String COVERAGE_NO_DOUBLE = "cov123"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${COVERAGE}\n" +
                        "${COVERAGE_NO_DOUBLE}\n" +
                        "\n" +
                        "10.56565\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "The coverage '${COVERAGE_NO_DOUBLE}' should be a double number.", "At least one coverage is not a double number."),
        ]

        when:
        new CoverageValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
