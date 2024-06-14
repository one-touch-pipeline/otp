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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class Md5sumFormatValidatorSpec extends Specification {

    void 'validate concerning fastq metadata, when column is missing, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        when:
        new Md5sumFormatValidator().validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Required column 'MD5' is missing.")
    }

    @Unroll
    void 'validate concerning bam metadata, when column is missing and linksource is #linksource, adds #warnlevel'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""", [
                linkSourceFiles: linksource
        ])

        when:
        new Md5sumFormatValidator().validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == warnlevel
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains(warntext)

        where:
        linksource || warnlevel     | warntext
        false      || LogLevel.WARNING | "Optional column 'MD5' is missing."
        true       || LogLevel.ERROR   | "If source files should only linked, the column '${MetaDataColumn.MD5.name()}' is required."
    }

    void 'validate concerning metadata, adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MD5}
xxxxyyyyxxxxyyyyxxxxyyyyxxxxyyyy
aaaabbbbaaaabbbbaaaabbbbaaaabbbb
xxxxyyyyxxxxyyyyxxxxyyyyxxxxyyyy
""")

        when:
        new Md5sumFormatValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains("Not a well-formatted MD5 sum: 'xxxxyyyyxxxxyyyyxxxxyyyyxxxxyyyy'.")
    }

    void 'validate concerning bam metadata, if cell is empty and linksource is false, then no problem is added'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${BamMetadataColumn.MD5}

""", [
                linkSourceFiles: false,
        ])

        when:
        new Md5sumFormatValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate concerning bam metadata, if cell is empty and linksource is true, then add error'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${BamMetadataColumn.MD5}

""", [
                linkSourceFiles: true,
        ])
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "The md5sum is required, if the files should only be linked"),
        ]

        when:
        new Md5sumFormatValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
