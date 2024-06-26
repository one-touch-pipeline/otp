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
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.ValidatorHelperService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class TagmentationLibrarySeqTypeValidatorSpec extends Specification {

    void 'validate, when CUSTOMER_LIBRARY and SEQUENCING_TYPE are missing, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
        )
        TagmentationLibrarySeqTypeValidator validator = new TagmentationLibrarySeqTypeValidator()
        validator.validatorHelperService = new ValidatorHelperService()

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when SEQUENCING_TYPE is missing, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\n" +
                        ""
        )
        TagmentationLibrarySeqTypeValidator validator = new TagmentationLibrarySeqTypeValidator()
        validator.validatorHelperService = new ValidatorHelperService()

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }

    @Unroll
    void 'validate, valid CUSTOMER_LIBRARY SEQUENCING_TYPE combinations (#seqTypeName, #library), succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\t${SEQUENCING_TYPE}\n" +
                        "${library}\t${seqTypeName}\n"
        )
        TagmentationLibrarySeqTypeValidator validator = new TagmentationLibrarySeqTypeValidator()
        validator.validatorHelperService = new ValidatorHelperService()

        when:
        validator.validate(context)

        then:
        context.problems.empty

        where:
        seqTypeName                             | library
        'seqtype'                               | ''
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | '4'
    }

    @Unroll
    void 'validate, invalid CUSTOMER_LIBRARY SEQUENCING_TYPE combinations (#seqTypeName, #library), show warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${TAGMENTATION_LIBRARY}\t${SEQUENCING_TYPE}\n" +
                        "${library}\t${seqTypeName}\n"
        )
        TagmentationLibrarySeqTypeValidator validator = new TagmentationLibrarySeqTypeValidator()
        validator.validatorHelperService = new ValidatorHelperService()

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, level, message1, message2),
        ]
        assertContainSame(context.problems, expectedProblems)

        where:
        seqTypeName                             | library | level            || message1                                                                                                                                           || message2
        "seqtype"                               | "5"     | LogLevel.WARNING || "The tagmentation library '5' in column ${TAGMENTATION_LIBRARY} indicates tagmentation, but the sequencing type 'seqtype' is without tagmentation" || TagmentationLibrarySeqTypeValidator.LIBRARY_WITHOUT_TAGMENTATION
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | ""      | LogLevel.ERROR   || "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there should be a value in the ${TAGMENTATION_LIBRARY} column."      || TagmentationLibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY
    }

    void 'validate, when CUSTOMER_LIBRARY is missing and SEQUENCING_TYPE has TAGMENTATION, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                        "seqtype${SeqType.TAGMENTATION_SUFFIX}\n"
        )
        TagmentationLibrarySeqTypeValidator validator = new TagmentationLibrarySeqTypeValidator()
        validator.validatorHelperService = new ValidatorHelperService()

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there " +
                                "should be a value in the ${TAGMENTATION_LIBRARY} column.",
                        TagmentationLibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY),
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}
