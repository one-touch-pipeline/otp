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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.MAXIMAL_READ_LENGTH

class MaximalReadLengthValidatorSpec extends Specification {

    @Unroll
    void 'validate, when column is missing and linksource is #linksource, adds #warnlevel'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext([
                linkSourceFiles: linksource
        ])
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), warnlevel, warntext)
        ]

        when:
        new MaximalReadLengthValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)

        where:
        linksource || warnlevel     | warntext
        false      || LogLevel.WARNING | "Optional column '${MAXIMAL_READ_LENGTH}' is missing."
        true       || LogLevel.ERROR   | "If source files should only linked, the column '${MAXIMAL_READ_LENGTH}' is required."
    }

    @Unroll
    void 'validate context with errors (linksource is: #linksource)'() {
        given:
        final String INVALID_DOUBLE = '456.789'
        final String INVALID_CHARS = 'abc'

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                [
                        MAXIMAL_READ_LENGTH,
                        '123',
                        '',
                        INVALID_DOUBLE,
                        INVALID_CHARS,
                ].join('\n'), [
                linkSourceFiles: linksource
        ]
        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.ERROR,
                        "The maximalReadLength '${INVALID_DOUBLE}' should be an integer number.", "At least one maximalReadLength is not an integer number."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, LogLevel.ERROR,
                        "The maximalReadLength '${INVALID_CHARS}' should be an integer number.", "At least one maximalReadLength is not an integer number."),
        ]
        if (linksource) {
            expectedProblems << new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                    "The maximalReadLength is required, if the files should only be linked")
        }

        when:
        new MaximalReadLengthValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)

        where:
        linksource || _
        false      || _
        true       || _
    }
}
