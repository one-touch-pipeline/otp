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
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class LibrarySeqTypeValidatorSpec extends Specification {

    void 'validate, when CUSTOMER_LIBRARY and SEQUENCING_TYPE are missing, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        context.problems.empty
    }

    void 'validate, when SEQUENCING_TYPE is missing, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        ""
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        context.problems.empty
    }

    @Unroll
    void 'validate, valid CUSTOMER_LIBRARY SEQUENCING_TYPE combinations (#seqTypeName, #tagmentation, #library), succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\t${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "${library}\t${seqTypeName}\t${tagmentation}\n"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)

        then:
        context.problems.empty

        where:
        seqTypeName                             | tagmentation | library
        'seqtype'                               | ''           | ''
        'seqtype'                               | 'true'       | '1'
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | ''           | '4'
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | 'true'       | '4'
    }

    @Unroll
    void 'validate, invalid CUSTOMER_LIBRARY SEQUENCING_TYPE combinations (#seqTypeName, #tagmentation, #library), show warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\t${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "${library}\t${seqTypeName}\t${tagmentation}\n"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, level, message1, message2),
        ]
        assertContainSame(context.problems, expectedProblems)

        where:
        seqTypeName                             | tagmentation | library | level         || message1                                                                                                                                  || message2
        "seqtype"                               | ""           | "5"     | Level.WARNING || "The library '5' in column ${CUSTOMER_LIBRARY} indicates tagmentation, but the seqtype 'seqtype' is without tagmentation"                 || LibrarySeqTypeValidator.LIBRARY_WITHOUT_TAGMENTATION
        "seqtype"                               | "true"       | ""      | Level.ERROR   || "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there should be a value in the ${CUSTOMER_LIBRARY} column." || LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | ""           | ""      | Level.ERROR   || "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there should be a value in the ${CUSTOMER_LIBRARY} column." || LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | "true"       | ""      | Level.ERROR   || "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there should be a value in the ${CUSTOMER_LIBRARY} column." || LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY
    }


    void 'validate, when CUSTOMER_LIBRARY is missing and SEQUENCING_TYPE has TAGMENTATION, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "seqtype\ttrue\n" +
                        "seqtype${SeqType.TAGMENTATION_SUFFIX}\t\n"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there " +
                                "should be a value in the ${CUSTOMER_LIBRARY} column.",
                        LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there " +
                                "should be a value in the ${CUSTOMER_LIBRARY} column.",
                        LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY),
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}
