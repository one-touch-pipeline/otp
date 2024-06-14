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
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class BarcodeValidatorSpec extends Specification {

    void 'validate, when barcode is valid, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.INDEX}\n" +
                        "\n" +
                        "ACAGTG\n" +
                        "CAGATC\n" +
                        "CAGATCGATC-ACAGTGGATC\n" +
                        "CAGATCGATC-ACAGTGGATC\n" +
                        "CAGATCNNNNN-ACAGTGNNNN\n" +
                        "CAGATCGATC-ACAGTGGATC-CAGATCGATC-ACAGTGGATC\n" +
                        "CAGATCGATC-ACAGTGGATC-CAGATCGATC-ACAGTGGATC\n" +
                        "CAGATCGATC,ACAGTGGATC,CAGATCGATC,ACAGTGGATC\n" +
                        "CAGATCGATC,ACAGTGGATC,CAGATCGATC,ACAGTGGATC\n"
        )

        when:
        new BarcodeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when barcode use valid chars, if not, add error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.INDEX}\n" +
                        "invalidBarcode\n"
        )

        when:
        new BarcodeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("'invalidBarcode' is not a well-formed barcode. It must match the regular expression '${BarcodeValidator.MUST_REGEX}'.")
    }

    void 'validate, when barcode contains invalid chars, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.INDEX}\n" +
                        "${barcode}\n"
        )

        when:
        new BarcodeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("'${barcode}' is not a well-formed barcode. It must match the regular expression '${BarcodeValidator.MUST_REGEX}'.")

        where:
        barcode << ['%', '$', '^', '_']
    }

    void 'validate, when no INDEX column exists in metadata file, adds warnings'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new BarcodeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem([] as Set, LogLevel.WARNING, "Optional column 'INDEX' is missing. OTP will try to parse the barcodes from the filenames.")
        ]
        containSame(context.problems, expectedProblems)
    }
}
