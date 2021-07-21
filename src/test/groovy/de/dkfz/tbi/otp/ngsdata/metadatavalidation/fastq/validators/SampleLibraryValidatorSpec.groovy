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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_LIBRARY
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME

class SampleLibraryValidatorSpec extends Specification {

    void 'validate, when SAMPLE_NAME and CUSTOMER_LIBRARY are missing, add error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.ERROR,
                        "Required column 'SAMPLE_NAME' is missing.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when CUSTOMER_LIBRARY is missing, add warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\n" +
                "testSample\n" +
                "testSampleLib\n"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.WARNING, "For sample 'testSampleLib' which contains 'lib', there should be a value in the ${TAGMENTATION_LIBRARY} column.", "For samples which contain 'lib', there should be a value in the TAGMENTATION_LIBRARY column.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when SAMPLE_NAME is missing, add error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\n" +
                        "lib"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.ERROR,
                        "Required column 'SAMPLE_NAME' is missing.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, valid SAMPLE_NAME and CUSTOMER_LIBRARY combinations, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${TAGMENTATION_LIBRARY}\n" +
                        "testSampleLib\tlib\n" +
                        "testSample\tlib\n" +
                        "testSample\n" +
                        "testLIbSample\tlib\n"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when SAMPLE_NAME does contain "lib" and CUSTOMER_LIBRARY is empty, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${TAGMENTATION_LIBRARY}\n" +
                        "testSampleLib\n"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.WARNING, "For sample 'testSampleLib' which contains 'lib', there should be a value in the ${TAGMENTATION_LIBRARY} column.", "For samples which contain 'lib', there should be a value in the TAGMENTATION_LIBRARY column.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}
