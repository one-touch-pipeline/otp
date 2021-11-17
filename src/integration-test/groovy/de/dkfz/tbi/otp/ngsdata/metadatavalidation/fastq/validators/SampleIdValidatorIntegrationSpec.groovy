/*
 * Copyright 2011-2020 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.util.regex.Matcher

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class SampleIdValidatorIntegrationSpec extends Specification implements DomainFactoryCore {

    SampleIdentifierValidator validator = new SampleIdentifierValidator()

    void setup() {
        SampleIdentifierParser sampleIdentifierParser = new SampleIdentifierParser() {

            @Override
            ParsedSampleIdentifier tryParse(String sampleIdentifier) {
                Matcher match = sampleIdentifier =~ /(.*)#(.*)#(.*)/
                if (match.matches()) {
                    return new DefaultParsedSampleIdentifier(match.group(1), match.group(2), match.group(3), sampleIdentifier, null)
                }
                return null
            }

            @SuppressWarnings("UnusedMethodParameter")
            @Override
            boolean tryParsePid(String pid) {
                return true
            }

            @Override
            String tryParseSingleCellWellLabel(String sampleIdentifier) {
                return null
            }
        }

        ValidatorHelperService validatorHelperService = new ValidatorHelperService()
        validatorHelperService.seqTypeService = new SeqTypeService()
        validator.validatorHelperService = validatorHelperService
        validator.sampleIdentifierService = Spy(SampleIdentifierService) {
            _ * getSampleIdentifierParser(_) >> { SampleIdentifierParserBeanName sampleIdentifierParserBeanName ->
                sampleIdentifierParser
            }
        }
    }

    void 'validate, succeed when sampleIdentifier not registered yet in combination with given seqType and pid'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_NAME}\t${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.SEQUENCING_READ_TYPE}" +
                        "\t${MetaDataColumn.BASE_MATERIAL}\t${MetaDataColumn.PROJECT}\n" +
                        "aBrandNewSampleId\taBrandNewSeqType\tsomeValue\tsomeValue\tsomeValue"
        )

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, succeed when using the same sampleIdentifier for the same pid with a new seqType'() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_NAME}\t${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.SEQUENCING_READ_TYPE}" +
                "\t${MetaDataColumn.BASE_MATERIAL}\t${MetaDataColumn.PROJECT}\n" +
                        "${seqTrack.sampleIdentifier}\taBrandNewSeqType\tsomeValue\tsomevalue\tsomeValue\n" +
                        "aBrandNewSampleId\taBrandNewSeqType2\tsomeValue2\tsomeValue\tsomeValue"
        )

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, fails when sampleIdentifier is already registered'() {
        given:
        Sample sample = createSample()
        SampleIdentifier sampleIdentifier = createSampleIdentifier(
                sample: sample,
                name: "mock1",
        )
        SeqTrack seqTrack = createSeqTrack(
                sample: sample,
                sampleIdentifier: sampleIdentifier.name,
        )

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_NAME}\t${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.PROJECT}\t${MetaDataColumn.SEQUENCING_READ_TYPE}" +
                "\t${MetaDataColumn.BASE_MATERIAL}\n" +
                        "${seqTrack.sampleIdentifier}\t${seqTrack.seqType.name}\t${seqTrack.project.name}\t${seqTrack.seqType.libraryLayout}\tsomeValue\n" +
                        "aBrandNewSampleId\taBrandNewSeqType\taBrandNewProject\taBrandNewReadType\tsomeValue"
        )

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        containSame(problem.affectedCells*.cellAddress, ["A2", "B2", "D2", "E2", "C2"])
        problem.message.contains("Sample Name '${seqTrack.sampleIdentifier}' is already registered for another sample with the same pid and seq type.")
    }

    void 'validate, fails when parsed sampleIdentifier is already registered'() {
        given:
        Project project = createProject(
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP,
        )
        Individual individual = createIndividual(
                project: project,
        )
        Sample sample = createSample(
                individual: individual,
        )
        SeqTrack seqTrack = createSeqTrack(
                sampleIdentifier: project.name + '#' + individual.pid + '#' + sample.sampleType.name,
                sample: sample,
        )

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SAMPLE_NAME}\t${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.PROJECT}\t${MetaDataColumn.SEQUENCING_READ_TYPE}" +
                        "\t${MetaDataColumn.BASE_MATERIAL}\n" +
                        "${project.name}#${seqTrack.individual.pid}#${seqTrack.sampleType.name}\t${seqTrack.seqType.name}\t${seqTrack.project.name}\t${seqTrack.seqType.libraryLayout}\tsomeValue\n" +
                        "aBrandNewSampleId\taBrandNewSeqType\taBrandNewProject\taBrandNewReadType\tsomeValue"
        )

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        containSame(problem.affectedCells*.cellAddress, ["A2", "B2", "D2", "E2", "C2"])
        problem.message.contains("Sample Name '${seqTrack.sampleIdentifier}' is already registered for another sample with the same pid and seq type.")
    }
}
