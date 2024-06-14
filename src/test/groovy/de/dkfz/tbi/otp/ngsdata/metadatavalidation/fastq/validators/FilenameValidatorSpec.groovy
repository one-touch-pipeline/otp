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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class FilenameValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FileType,
        ]
    }

    void 'validate context with errors and warnings'() {
        given:
        createFileType(signature: '_fastq')
        createFileType(signature: '.fastq')
        MetadataValidationContext context = MetadataValidationContextFactory.createContext([
            "${MetaDataColumn.FASTQ_FILE}",
            "test_fastq.gz",
            "test.fastq.gz",
            "/tmp_underscore/test.fastq.gz",
            "test_fastq.gzz",
            "test_fast.gz",
            "öäü_fastq.gz",
            "/tmp/test_fastq.gz",
        ].join("\n"))

        String charList = FilenameValidator.requiredCharactersAsReadableList
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.WARNING,
                        "Filename 'test.fastq.gz' does not contain all required characters: ${charList}", "At least one filename does not contain all required characters."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.WARNING,
                        "Filename '/tmp_underscore/test.fastq.gz' does not contain all required characters: ${charList}", "At least one filename does not contain all required characters."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, LogLevel.ERROR,
                        "Filename 'test_fastq.gzz' does not end with '.gz'.", "At least one filename does not end with '.gz'."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, LogLevel.ERROR,
                        "Filename 'test_fast.gz' contains neither '_fastq' nor '.fastq'.", "At least one filename contains neither '_fastq' nor '.fastq'."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, LogLevel.ERROR,
                        "Filename 'öäü_fastq.gz' contains invalid characters.", "At least one filename contains invalid characters."),
        ]

        when:
        new FilenameValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void 'validate context without FASTQ_FILE column'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new FilenameValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        problem.message == "Required column 'FASTQ_FILE' is missing."
    }
}
