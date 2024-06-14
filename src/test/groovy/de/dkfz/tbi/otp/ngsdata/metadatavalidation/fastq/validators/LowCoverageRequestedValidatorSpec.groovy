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
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.ngsdata.ValidatorHelperService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.LOW_COVERAGE_REQUESTED
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_READ_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class LowCoverageRequestedValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqType,
        ]
    }

    LowCoverageRequestedValidator validator = new LowCoverageRequestedValidator([
            validatorHelperService        : new ValidatorHelperService([seqTypeService: new SeqTypeService()]),
    ])

    @Unroll
    void "validate, when column LOW_COVERAGE_REQUESTED is '#lowCov', then returns problems should be empty: #lowCov"() {
        given:
        DomainFactory.createWholeGenomeSeqType()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${LOW_COVERAGE_REQUESTED}\nWHOLE_GENOME\tPAIRED\t${lowCov}")

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty() == isEmpty

        where:
        lowCov   | isEmpty
        ''       | true
        'true'   | true
        'false'  | true
        'null'   | false
        '12345'  | false
    }

    void "validate, if it is not WHOLE_GENOME then error"() {
        given:
        SeqType seqType = DomainFactory.createExomeSeqType()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${LOW_COVERAGE_REQUESTED}\n${seqType.name}\t${seqType.libraryLayout}\ttrue")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR, "Low coverage value is set but the sequencing type '${seqType.displayNameWithLibraryLayout}' is not from Type '${SeqTypeNames.WHOLE_GENOME.seqTypeName}'", "low coverage set for non ${SeqTypeNames.WHOLE_GENOME.seqTypeName} data"),
        ]

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
