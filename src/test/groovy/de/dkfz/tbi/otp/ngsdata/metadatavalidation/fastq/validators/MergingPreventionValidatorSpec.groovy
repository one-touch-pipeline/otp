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
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.util.regex.Matcher

import static de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName.NO_ALIGNMENT
import static de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName.OTP_ALIGNMENT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MergingPreventionValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                Individual,
                LibraryPreparationKit,
                Pipeline,
                ProcessingOption,
                Project,
                Realm,
                ReferenceGenome,
                Sample,
                SampleIdentifier,
                SampleType,
        ]
    }


    void "test validate"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createCellRangerAlignableSeqTypes()

        Project project = DomainFactory.proxyCore.createProject(
                alignmentDeciderBeanName: OTP_ALIGNMENT,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP,
        )

        Sample scSample = DomainFactory.proxyCore.createSample()
        scSample.individual.project = project
        scSample.individual.save(flush: true)
        SeqType scSeqType = DomainFactory.proxyCellRanger.createSeqType()
        DomainFactory.proxyCellRanger.createMergingWorkPackage(
                sample: scSample,
                seqType: scSeqType,
        )
        DomainFactory.createSampleIdentifier(sample: scSample, name: "sample1")

        Sample bulkSample = DomainFactory.proxyCore.createSample()
        bulkSample.individual.project = project
        bulkSample.individual.save(flush: true)
        SeqType bulkSeqType = DomainFactory.proxyRoddy.createSeqType()
        DomainFactory.proxyRoddy.createMergingWorkPackage(
                sample: bulkSample,
                seqType: bulkSeqType,
        )
        DomainFactory.createSampleIdentifier(sample: bulkSample, name: "sample2")

        SeqType bulkSeqTypeWithAntibodyTarget = DomainFactory.createChipSeqType()
        AntibodyTarget antibodyTarget = DomainFactory.proxyCore.createAntibodyTarget()
        DomainFactory.proxyRoddy.createMergingWorkPackage(
                sample: bulkSample,
                seqType: bulkSeqTypeWithAntibodyTarget,
                antibodyTarget: antibodyTarget,
        )
        DomainFactory.createSampleIdentifier(sample: bulkSample, name: "sample3")

        Sample unusedSample = DomainFactory.proxyCore.createSample()
        DomainFactory.createSampleIdentifier(sample: unusedSample, name: "unusedSample")

        Sample sampleNoAlignment = DomainFactory.proxyCore.createSample()
        sampleNoAlignment.individual.project.alignmentDeciderBeanName = NO_ALIGNMENT
        sampleNoAlignment.individual.project.save(flush: true)
        DomainFactory.proxyRoddy.createMergingWorkPackage(
                sample: sampleNoAlignment,
                seqType: bulkSeqType,
        )
        DomainFactory.createSampleIdentifier(sample: sampleNoAlignment, name: "sampleNoAlignment")


        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${SEQUENCING_TYPE}\t${LIBRARY_LAYOUT}\t${PROJECT}\t${BASE_MATERIAL}\t${ANTIBODY_TARGET}\n" +
                        "sample1\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t\t${SeqType.SINGLE_CELL_DNA}\n" +
                        "sample2\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t\n" +
                        "sample3\t${bulkSeqTypeWithAntibodyTarget.name}\t${bulkSeqTypeWithAntibodyTarget.libraryLayout.name()}\t\t\t${antibodyTarget.name}\n" +
                        "${project.name}ß${scSample.individual.pid}ß${scSample.sampleType.name}\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t${project.name}\t${SeqType.SINGLE_CELL_DNA}\n" +
                        "${project.name}ß${bulkSample.individual.pid}ß${bulkSample.sampleType.name}\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t${project.name}\n" +
                        "unusedSample\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t\n" +
                        "sampleNoAlignment\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t\n" +
                        "unknown_sample\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t\t${SeqType.SINGLE_CELL_DNA}\n" +
                        "sample1\tunknown\t${scSeqType.libraryLayout.name()}\t\t${SeqType.SINGLE_CELL_DNA}\n" +
                        "sample1\t${scSeqType.name}\tunknown\t\t${SeqType.SINGLE_CELL_DNA}\n" +
                        "sample1\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t\tunknown\n" +
                        "sample3\t${bulkSeqTypeWithAntibodyTarget.name}\t${bulkSeqTypeWithAntibodyTarget.libraryLayout.name()}\t\t\tunknown\n" +
                        "${project.name}ß${scSample.individual.pid}ß${scSample.sampleType.name}\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\tunknown\t${SeqType.SINGLE_CELL_DNA}\n"
        )

        Validator<MetadataValidationContext> validator = new MergingPreventionValidator(
                antibodyTargetService: new AntibodyTargetService(),
                seqTypeService: new SeqTypeService(),
                sampleIdentifierService: [
                        getSampleIdentifierParser: { SampleIdentifierParserBeanName sampleIdentifierParserBeanName ->
                            new SampleIdentifierParser() {

                                @Override
                                ParsedSampleIdentifier tryParse(String sampleIdentifier) {
                                    Matcher match = sampleIdentifier =~ /(.*)ß(.*)ß(.*)/
                                    if (match.matches()) {
                                        return new DefaultParsedSampleIdentifier(match.group(1), match.group(2), match.group(3), sampleIdentifier, null)
                                    } else {
                                        return null
                                    }
                                }

                                @SuppressWarnings("UnusedMethodParameter")
                                @Override
                                boolean tryParsePid(String pid) {
                                    return true
                                }

                                @Override
                                String tryParseCellPosition(String sampleIdentifier) {
                                    return null
                                }
                            }
                        }
                ] as SampleIdentifierService,
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample ${scSample.displayName} with sequencing type ${scSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.WARNING,
                        "Sample ${bulkSample.displayName} with sequencing type ${bulkSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "Sample ${bulkSample.displayName} with sequencing type ${bulkSeqTypeWithAntibodyTarget.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "Sample ${scSample.displayName} with sequencing type ${scSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.WARNING,
                        "Sample ${bulkSample.displayName} with sequencing type ${bulkSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
        ]
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
