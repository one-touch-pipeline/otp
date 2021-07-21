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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

class WithdrawnValidatorSpec extends Specification {

    void 'validate adds expected errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.WITHDRAWN}\n" +
                        "YES\n" +
                        "TRUE\n" +
                        "1\n" +
                        "-1\n" +
                        "\n" +
                        "0\n" +
                        "none\n" +
                        "NONE\n" +
                        "None\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "'YES' is not an acceptable '${MetaDataColumn.WITHDRAWN.name()}' value. It must be empty or '0' or 'None'. Withdrawn data cannot be imported into OTP.", "Withdrawn data cannot be imported into OTP."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        "'TRUE' is not an acceptable '${MetaDataColumn.WITHDRAWN.name()}' value. It must be empty or '0' or 'None'. Withdrawn data cannot be imported into OTP.", "Withdrawn data cannot be imported into OTP."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.ERROR,
                        "'1' is not an acceptable '${MetaDataColumn.WITHDRAWN.name()}' value. It must be empty or '0' or 'None'. Withdrawn data cannot be imported into OTP.", "Withdrawn data cannot be imported into OTP."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, LogLevel.ERROR,
                        "'-1' is not an acceptable '${MetaDataColumn.WITHDRAWN.name()}' value. It must be empty or '0' or 'None'. Withdrawn data cannot be imported into OTP.", "Withdrawn data cannot be imported into OTP."),
        ]

        when:
        new WithdrawnValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when metadata does not contain a column WITHDRAWN, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new WithdrawnValidator().validate(context)

        then:
        context.problems.empty
    }
}
