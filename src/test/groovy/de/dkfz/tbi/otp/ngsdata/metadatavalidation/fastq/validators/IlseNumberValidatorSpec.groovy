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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class IlseNumberValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                IlseSubmission,
        ]
    }

    void 'validate, when metadata fields contain valid ILSe number, succeeds'() {
        given:
        int ILSE_NO = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" +
                        "${ILSE_NO}\n" +
                        "${ILSE_NO}\n",
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata does not contain a column ILSE_NO, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column ILSE_NO is empty, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                "\n"
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata fields contain more than one ILSe number, adds warnings'() {
        given:
        int ILSE_NO_1 = 5461
        int ILSE_NO_2 = 5462
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO_1}\n" +
                        "${ILSE_NO_2}\n",
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/${ILSE_NO_1}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, LogLevel.INFO, "There are multiple ILSe numbers in the metadata file.", "There are multiple ILSe numbers in the metadata file."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.WARNING, "The metadata file path '${context.metadataFile}' does not contain the ILSe number '${ILSE_NO_2}'.", "At least one metadata file path does not contain the ILSe number."),
                ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when ILSe number is not an int, adds errors'() {
        given:
        String ILSE_NO = "ilseNu"
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                "${ILSE_NO}\n" +
                "${ILSE_NO}\n",
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR, "The ILSe number 'ilseNu' is not an integer.", "At least one ILSe number is not an integer."),
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when ILSe number already exist, adds warnings'() {
        given:
        int ILSE_NO = 5464
        DomainFactory.createIlseSubmission(
                ilseNumber: ILSE_NO,
        )
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" +
                        "${ILSE_NO}\n",
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]
        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, LogLevel.INFO, "The ILSe number '${ILSE_NO}' already exists.", "At least one ILSe number already exists.")
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when file path does not contain the ILSe number, adds warnings'() {
        given:
        int ILSE_NO = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" ,
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]

        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.WARNING, "The metadata file path '${context.metadataFile}' does not contain the ILSe number '${ILSE_NO}'.", "At least one metadata file path does not contain the ILSe number.")
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when file path contains the ILSe number, succeeds'() {
        given:
        int ILSE_NO = 5464
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.ILSE_NO}\n" +
                        "${ILSE_NO}\n" ,
                ["metadataFile": Paths.get("${TestCase.uniqueNonExistentPath}/${ILSE_NO}/run${HelperUtils.uniqueString}/metadata_fastq.tsv")]

        )

        when:
        new IlseNumberValidator().validate(context)

        then:
        context.problems.empty
    }
}
