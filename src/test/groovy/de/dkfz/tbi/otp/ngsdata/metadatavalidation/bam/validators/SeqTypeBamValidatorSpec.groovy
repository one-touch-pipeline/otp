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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class SeqTypeBamValidatorSpec extends Specification implements DataTest {

    SeqTypeBamValidator service

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqType,
        ]
    }

    void setup() {
        service = new SeqTypeBamValidator()
        service.seqTypeService = new SeqTypeService()
    }

    void 'validate, when column SEQUENCING_TYPE missing, then add expected problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "SomeColumn\n"
                        +
                        "SomeValue"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.ERROR,
                        "Required column '${SEQUENCING_TYPE}' is missing.")
        ]

        when:
        service.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when column exist and seqType is registered in OTP, succeeds'() {
        given:
        String SEQ_TYPE_NAME = "seqTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                        "${SEQ_TYPE_NAME}\n"
        )

        DomainFactory.createSeqType([name: SEQ_TYPE_NAME])

        when:
        service.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist and seqType is registered in OTP as import alias, succeeds'() {
        given:
        String SEQ_TYPE_NAME = "seqTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                        "${SEQ_TYPE_NAME}\n"
        )

        DomainFactory.createDomainWithImportAlias(SeqType,
                [
                        importAlias: [SEQ_TYPE_NAME],
                        displayName  : SEQ_TYPE_NAME,
                        dirName      : "seqTypeDirName",
                        roddyName    : null,
                        libraryLayout: SequencingReadType.SINGLE,
                        singleCell   : false,
        ])

        when:
        service.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist but seqType is not registered in OTP, adds problems'() {
        given:
        String SEQ_TYPE_NAME = "seqTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                        "${SEQ_TYPE_NAME}\n"
        )

        when:
        service.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The sequencing type '${SEQ_TYPE_NAME}' is not registered in OTP.")
    }

    void 'validate, when column exist but seqType is not given, adds problems'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                        "\n"
        )

        when:
        service.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("No seqType is given")
    }
}
