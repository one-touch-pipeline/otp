/*
 * Copyright 2011-2023 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SampleIdentifierService
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.ngsdata.ValidatorHelperService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class SampleLowCoverageRequestedValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                SeqType,
                Project,
                SampleIdentifier,
                SampleType,
                ProcessingOption,
        ]
    }

    SampleLowCoverageRequestedValidator validator = new SampleLowCoverageRequestedValidator([
            validatorHelperService        : new ValidatorHelperService([seqTypeService: new SeqTypeService()]),
            sampleIdentifierService : new SampleIdentifierService()],)

    @Unroll
    void "validate, that the sequence has no multiple LOW_COVERAGE_REQUESTED values"() {
        given:
        DomainFactory.createWholeGenomeSeqType()
        DomainFactory.createExomeSeqType()
        DomainFactory.createSampleIdentifier(name: 'SAMPLE_NAME')
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${PROJECT}\t${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${LOW_COVERAGE_REQUESTED}\n" +
                        "PROJECT\tSAMPLE_NAME\tWHOLE_GENOME\tPAIRED\t${lowCov}\n" +
                        "PROJECT\tSAMPLE_NAME\tWHOLE_GENOME\tPAIRED\t${lowCovComparison}\n" +
                        "PROJECT\tSAMPLE_NAME\tWHOLE_GENOME\tPAIRED\t${lowCov2}\n" +
                        "PROJECT1\tSAMPLE_NAME\tEXOME\tPAIRED\t${lowCov}\n" +
                        "PROJECT1\tSAMPLE_NAME\tEXOME\tPAIRED\t${lowCovComparison}\n" +
                        "PROJECT1\tSAMPLE_NAME\tEXOME\tPAIRED\t${lowCov2}")

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty() == isEmpty

        where:
        lowCov  | lowCovComparison | lowCov2  | isEmpty
        'true'  | 'true'           | 'true'   | true
        'false' | 'false'          | 'false'  | true
        'null'  | 'null'           | 'null'   | true
        ''      | ''               | ''       | true
        'true'  | 'false'          | 'null'   | false
        ''      | 'null'           | 'true'   | false
    }

    void "validate, if error gives the expected errormessage"() {
        given:
        SeqType seqType = DomainFactory.createExomeSeqType()
        Individual individual = DomainFactory.createIndividual()
        Sample sample = DomainFactory.createSample([individual: individual])
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier([sample: sample])
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${PROJECT}\t${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${LOW_COVERAGE_REQUESTED}\n" +
                        "${individual.project.name}\t${sampleIdentifier.name}\t${seqType.name}\t${seqType.libraryLayout}\ttrue\n" +
                        "${individual.project.name}\t${sampleIdentifier.name}\t${seqType.name}\t${seqType.libraryLayout}\tfalse")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0, 1]*.cells.flatten() as Set<Cell>, LogLevel.ERROR,
                        "Sample ${individual.pid} ${sample.sampleType} with sequencing type ${seqType.displayNameWithLibraryLayout} has multiple ${LOW_COVERAGE_REQUESTED.name()} values.",
                        "Sample with multiple ${LOW_COVERAGE_REQUESTED.name()} values exists."),
        ]

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
