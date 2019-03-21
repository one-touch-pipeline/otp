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
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.LIBRARY_PREPARATION_KIT
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class LibraryPreparationKitValidatorSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            LibraryPreparationKit,
    ]}

    void 'validate, when library preparation kit is not registered in OTP, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE}\t${LIBRARY_PREPARATION_KIT}
EXON\tIndividual1
""")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The ${LIBRARY_PREPARATION_KIT} 'Individual1' is not registered in OTP.",
                        "At least one ${LIBRARY_PREPARATION_KIT} is not registered in OTP.")
        ]

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when library preparation kit is registered in OTP and is EXOME, succeeds'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE}\t${LIBRARY_PREPARATION_KIT}
EXON\tIndividual1\t
""")

        DomainFactory.createLibraryPreparationKit([name: "Individual1"])

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, without library preparation kit but EXOME, adds problems'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\n" +
                        "EXON\n"
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "The ${SEQUENCING_TYPE} is 'EXON' but no ${LIBRARY_PREPARATION_KIT} is given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.",
                        "If the ${SEQUENCING_TYPE} is '${SeqTypeNames.EXOME.seqTypeName}' the ${LIBRARY_PREPARATION_KIT} should be given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.")
        ]

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, without library preparation kit and without EXOME, succeeds'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\n" +
                        "seqType\n"
        )

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        context.problems.empty
    }
}
