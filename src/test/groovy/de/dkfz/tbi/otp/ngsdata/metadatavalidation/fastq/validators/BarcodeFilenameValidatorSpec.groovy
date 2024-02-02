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
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INDEX

class BarcodeFilenameValidatorSpec extends Specification {

    void 'validate, when column barcode does not exist in the metadata file and barcode can not be parsed from filename, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                        "testFileName.fastq.gz\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column barcode does not exist in the metadata file and barcode can be parsed from filename, adds warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                        "testFileName_AGGCAGAA_1.fastq.gz\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.WARNING, "The ${INDEX} column is missing. OTP will use the barcode 'AGGCAGAA' parsed from filename 'testFileName_AGGCAGAA_1.fastq.gz'. (For multiplexed lanes the ${INDEX} column should be filled.)", "The INDEX column is missing")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when column barcode is empty and barcode can not be parsed from filename, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${INDEX}\n" +
                        "testFileName.fastq.gz\t\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column barcode is empty and barcode can be parsed from filename, adds warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${INDEX}\n" +
                        "testFileName_AGGCAGAA_1.fastq.gz\t\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.WARNING, "A barcode can be parsed from the filename 'testFileName_AGGCAGAA_1.fastq.gz', but the ${INDEX} cell is empty. OTP will ignore the barcode parsed from the filename.", "A barcode can be parsed from the filename, but the INDEX cell is empty. OTP will ignore the barcode parsed from the filename.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when barcode exists and barcode can not be parsed from filename, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${INDEX}\n" +
                        "testFileName.fastq.gz\tAGGCAGAA\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when barcode and barcode in filename match, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${INDEX}\n" +
                        "testFileName.fastq.gz\t\n" +
                        "testFileName_AGGCAGAA.fastq.gz\tAGGCAGAA\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when barcode and barcode in filename do not match, adds warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${INDEX}\n" +
                        "testFileName_AGGCAGGG_1.fastq.gz\tAGGCAGAA\n"
        )

        when:
        new BarcodeFilenameValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.WARNING, "The barcode parsed from the filename 'testFileName_AGGCAGGG_1.fastq.gz' ('AGGCAGGG') is different from the value in the ${INDEX} cell ('AGGCAGAA'). OTP will ignore the barcode parsed from the filename and use the barcode 'AGGCAGAA'.", "At least one barcode parsed from the filename is different from the value in the INDEX cell. OTP will ignore the barcode parsed from the filename.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}
