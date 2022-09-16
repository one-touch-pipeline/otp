/*
 * Copyright 2011-2021 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.parser.TestSampleIdentifierParser
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Integration
@Rollback
class MergingConflictsValidatorIntegrationSpec extends Specification implements DomainFactoryCore {

    MergingConflictsValidator validator

    void setup() {
        validator = new MergingConflictsValidator([
                validatorHelperService: new ValidatorHelperService([
                        antibodyTargetService  : new AntibodyTargetService(),
                        sampleIdentifierService: Spy(SampleIdentifierService) {
                            _ * getSampleIdentifierParser(_) >> { SampleIdentifierParserBeanName sampleIdentifierParserBeanName ->
                                new TestSampleIdentifierParser()
                            }
                        },
                        seqPlatformService     : new SeqPlatformService([
                                seqPlatformModelLabelService: new SeqPlatformModelLabelService(),
                                sequencingKitLabelService   : new SequencingKitLabelService()
                        ]),
                        seqTypeService         : new SeqTypeService(),
                ]),
        ])
    }

    void "validate fails with warnings when no seqPlatformGroups are defined"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createCellRangerAlignableSeqTypes()

        Project project = createProject(sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP)
        Individual individual = createIndividual(project: project)

        Sample sample1 = createSample(individual: individual)
        SeqType seqType1 = DomainFactory.proxyCellRanger.createSeqType()
        DomainFactory.createSampleIdentifier(sample: sample1, name: "sample1")

        Sample sample2 = createSample(individual: individual)
        SeqType seqType2 = DomainFactory.proxyRoddy.createSeqType()
        DomainFactory.createSampleIdentifier(sample: sample2, name: "sample2")

        String individual3 = "individual3"
        String sampleType3 = "sampleType3"
        SeqType seqType3 = DomainFactory.proxyRoddy.createSeqType()

        SeqPlatform seqPlatform = createSeqPlatform()
        SeqPlatform seqPlatform1 = createSeqPlatform()
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        String furtherValues = [
                seqPlatform.name,
                seqPlatform.seqPlatformModelLabel,
                "",
                libraryPreparationKit.name,
        ].join('\t')

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${PROJECT}\t${BASE_MATERIAL}\t${ANTIBODY_TARGET}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${LIB_PREP_KIT}\n" +
                        "sample1\t${seqType1.name}\t${seqType1.libraryLayout.name()}\t\t${SeqType.SINGLE_CELL_DNA}\t\t${furtherValues}\n" +
                        "sample1\t${seqType1.name}\t${seqType1.libraryLayout.name()}\t\t${SeqType.SINGLE_CELL_DNA}\t\t${furtherValues}\n" +
                        "sample2\t${seqType2.name}\t${seqType2.libraryLayout.name()}\t\t\t\t${furtherValues}\n" +
                        "sample2\t${seqType2.name}\t${seqType2.libraryLayout.name()}\t\t\t\t${furtherValues}\n" +
                        "sample2\t${seqType2.name}\t${seqType2.libraryLayout.name()}\t\t\t\t${seqPlatform1.name}\t${seqPlatform1.seqPlatformModelLabel}\t\t\n" +
                        "${project.name}#${individual3}#${sampleType3}\t${seqType3.name}\t${seqType3.libraryLayout.name()}\t${project.name}\t\t\t${furtherValues}\n" +
                        "${project.name}#${individual3}#${sampleType3}\t${seqType3.name}\t${seqType3.libraryLayout.name()}\t${project.name}\t\t\t${seqPlatform1.name}\t${seqPlatform1.seqPlatformModelLabel}\t\t\n"
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells + context.spreadsheet.dataRows[4].cells) as Set, LogLevel.WARNING,
                        "Sample ${sample2.individual.pid} ${sample2.sampleType.displayName} with sequencing type ${seqType2.displayNameWithLibraryLayout} cannot be merged with itself, since it uses incompatible seq platforms",
                        "Sample can not be merged with itself, since it uses incompatible seq platforms."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set, LogLevel.WARNING,
                        "Sample ${individual3} ${sampleType3} with sequencing type ${seqType2.displayNameWithLibraryLayout} cannot be merged with itself, since it uses incompatible seq platforms",
                        "Sample can not be merged with itself, since it uses incompatible seq platforms."),
        ]

        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void "validate with PROJECT_SEQ_TYPE_SPECIFIC seqPlatformGroup succeeds"() {
        given:
        Project project = createProject(sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP)
        Individual individual = createIndividual(project: project)
        Sample sample = createSample(individual: individual)
        SeqType seqType = createSeqType()
        DomainFactory.createSampleIdentifier(sample: sample, name: "sample")

        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()

        MergingCriteria mergingCriteria = createMergingCriteria([
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
                seqType            : seqType,
                project            : sample.project,
        ])

        createSeqPlatformGroup(mergingCriteria: mergingCriteria, seqPlatforms: [seqPlatform1, seqPlatform2])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${PROJECT}\t${BASE_MATERIAL}\t${ANTIBODY_TARGET}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${LIB_PREP_KIT}\n" +
                        "sample\t${seqType.name}\t${seqType.libraryLayout.name()}\t\t\t\t${seqPlatform1.name}\t${seqPlatform1.seqPlatformModelLabel}\t\t\n" +
                        "sample\t${seqType.name}\t${seqType.libraryLayout.name()}\t\t\t\t${seqPlatform2.name}\t${seqPlatform2.seqPlatformModelLabel}\t\t\n"
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = []
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void "validate with OTP_DEFAULT seqPlatformGroup succeeds"() {
        given:
        Project project = createProject(sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP)
        Individual individual = createIndividual(project: project)
        Sample sample = createSample(individual: individual)
        SeqType seqType = createSeqType()
        DomainFactory.createSampleIdentifier(sample: sample, name: "sample")

        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()

        createMergingCriteria([
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
                seqType            : seqType,
                project            : sample.project,
        ])

        createSeqPlatformGroup(seqPlatforms: [seqPlatform1, seqPlatform2])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${PROJECT}\t${BASE_MATERIAL}\t${ANTIBODY_TARGET}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${LIB_PREP_KIT}\n" +
                        "sample\t${seqType.name}\t${seqType.libraryLayout.name()}\t\t\t\t${seqPlatform1.name}\t${seqPlatform1.seqPlatformModelLabel}\t\t\n" +
                        "sample\t${seqType.name}\t${seqType.libraryLayout.name()}\t\t\t\t${seqPlatform2.name}\t${seqPlatform2.seqPlatformModelLabel}\t\t\n"
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = []
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void "validate with IGNORE_FOR_MERGING seqPlatformGroup succeeds"() {
        given:
        Project project = createProject(sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP)
        Individual individual = createIndividual(project: project)
        Sample sample = createSample(individual: individual)
        SeqType seqType = createSeqType()
        DomainFactory.createSampleIdentifier(sample: sample, name: "sample")

        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()

        createMergingCriteria([
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING,
                seqType            : seqType,
                project            : sample.project,
        ])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${PROJECT}\t${BASE_MATERIAL}\t${ANTIBODY_TARGET}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${LIB_PREP_KIT}\n" +
                        "sample\t${seqType.name}\t${seqType.libraryLayout.name()}\t\t\t\t${seqPlatform1.name}\t${seqPlatform1.seqPlatformModelLabel}\t\t\n" +
                        "sample\t${seqType.name}\t${seqType.libraryLayout.name()}\t\t\t\t${seqPlatform2.name}\t${seqPlatform2.seqPlatformModelLabel}\t\t\n"
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = []
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void "validate success without warning when seqType is not found"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createCellRangerAlignableSeqTypes()

        Project project = createProject(sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP)
        Individual individual = createIndividual(project: project)

        Sample sample1 = createSample(individual: individual)
        SeqType seqType1 = DomainFactory.proxyCellRanger.createSeqType()
        DomainFactory.createSampleIdentifier(sample: sample1, name: "sample1")

        SeqPlatform seqPlatform = createSeqPlatform()
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        String furtherValues = [
                seqPlatform.name,
                seqPlatform.seqPlatformModelLabel,
                "",
                libraryPreparationKit.name,
        ].join('\t')

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${PROJECT}\t${BASE_MATERIAL}\t${ANTIBODY_TARGET}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${LIB_PREP_KIT}\n" +
                        "sample1\tUnknownSeqType\t${seqType1.libraryLayout.name()}\t\t\t\t${furtherValues}\n"
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = []

        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
