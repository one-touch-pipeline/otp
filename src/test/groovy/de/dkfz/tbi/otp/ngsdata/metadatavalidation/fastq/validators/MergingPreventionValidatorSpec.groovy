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
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsAlignment
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.parser.TestSampleIdentifierParser
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName.NO_ALIGNMENT
import static de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName.OTP_ALIGNMENT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MergingPreventionValidatorSpec extends Specification implements DataTest, DomainFactoryCore, AlignmentPipelineFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                DataFile,
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

    void "validate, check different cases without warning and with sample exist warning"() {
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
        addSeqTrack(DomainFactory.proxyCellRanger.createMergingWorkPackage(
                sample: scSample,
                seqType: scSeqType,
        ))
        DomainFactory.createSampleIdentifier(sample: scSample, name: "sample1")
        createMergingCriteria([
                project: scSample.project,
                seqType: scSeqType,
        ])

        Sample bulkSample = DomainFactory.proxyCore.createSample()
        bulkSample.individual.project = project
        bulkSample.individual.save(flush: true)
        SeqType bulkSeqType = DomainFactory.proxyRoddy.createSeqType()
        MergingWorkPackage bulkMergingWorkPackage = DomainFactory.proxyRoddy.createMergingWorkPackage(
                sample: bulkSample,
                seqType: bulkSeqType,
        )
        DomainFactory.createSampleIdentifier(sample: bulkSample, name: "sample2")
        createMergingCriteria([
                project: bulkSample.project,
                seqType: bulkSeqType,
        ])

        SeqType bulkSeqTypeWithAntibodyTarget = DomainFactory.createChipSeqType()
        AntibodyTarget antibodyTarget = DomainFactory.proxyCore.createAntibodyTarget()
        MergingWorkPackage bulkMergingWorkPackageWithAntibodyTarget = DomainFactory.proxyRoddy.createMergingWorkPackage(
                sample: bulkSample,
                seqType: bulkSeqTypeWithAntibodyTarget,
                antibodyTarget: antibodyTarget,
        )
        DomainFactory.createSampleIdentifier(sample: bulkSample, name: "sample3")
        createMergingCriteria([
                project: bulkSample.project,
                seqType: bulkSeqTypeWithAntibodyTarget,
        ])

        Sample unusedSample = DomainFactory.proxyCore.createSample()
        DomainFactory.createSampleIdentifier(sample: unusedSample, name: "unusedSample")

        Sample sampleNoAlignment = DomainFactory.proxyCore.createSample()
        sampleNoAlignment.individual.project.alignmentDeciderBeanName = NO_ALIGNMENT
        sampleNoAlignment.individual.project.save(flush: true)
        MergingWorkPackage noAlignmentMergingWorkPackage = DomainFactory.proxyRoddy.createMergingWorkPackage(
                sample: sampleNoAlignment,
                seqType: bulkSeqType,
        )
        DomainFactory.createSampleIdentifier(sample: sampleNoAlignment, name: "sampleNoAlignment")

        SeqPlatform seqPlatform = createSeqPlatform()
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        //adapt mergingWorkPackages to be match the values used in the MetaDataSheet
        [
                bulkMergingWorkPackage,
                bulkMergingWorkPackageWithAntibodyTarget,
                noAlignmentMergingWorkPackage,
        ].each { MergingWorkPackage mergingWorkPackage ->
            mergingWorkPackage.seqPlatformGroup.seqPlatforms.add(seqPlatform)
            mergingWorkPackage.seqPlatformGroup.save(flush: true)
            mergingWorkPackage.libraryPreparationKit = libraryPreparationKit
            mergingWorkPackage.save(flush: true)
            addSeqTrack(mergingWorkPackage)
        }

        String furtherValues = [
                seqPlatform.name,
                seqPlatform.seqPlatformModelLabel,
                "",
                libraryPreparationKit.name,
        ].join('\t')

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${SEQUENCING_TYPE}\t${SEQUENCING_READ_TYPE}\t${PROJECT}\t${BASE_MATERIAL}\t${ANTIBODY_TARGET}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${SEQUENCING_KIT}\t${LIB_PREP_KIT}\n" +
                        "sample1\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t\t${SeqType.SINGLE_CELL_DNA}\t\t${furtherValues}\n" +
                        "sample2\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t\t\t\t${furtherValues}\n" +
                        "sample3\t${bulkSeqTypeWithAntibodyTarget.name}\t${bulkSeqTypeWithAntibodyTarget.libraryLayout.name()}\t\t\t${antibodyTarget.name}\t${furtherValues}\n" +
                        "${project.name}#${scSample.individual.pid}#${scSample.sampleType.name}\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t${project.name}\t${SeqType.SINGLE_CELL_DNA}\t\t${furtherValues}\n" +
                        "${project.name}#${bulkSample.individual.pid}#${bulkSample.sampleType.name}\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t${project.name}\t\t\t${furtherValues}\n" +
                        "unusedSample\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t\t\t\t${furtherValues}\n" +
                        "sampleNoAlignment\t${bulkSeqType.name}\t${bulkSeqType.libraryLayout.name()}\t\t\t\t${furtherValues}\n" +
                        "unknown_sample\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t\t\t${SeqType.SINGLE_CELL_DNA}\t${furtherValues}\n" +
                        "sample1\tunknown\t${scSeqType.libraryLayout.name()}\t\t${SeqType.SINGLE_CELL_DNA}\t\t${furtherValues}\n" +
                        "sample1\t${scSeqType.name}\tunknown\t\t${SeqType.SINGLE_CELL_DNA}\t\t${furtherValues}\n" +
                        "sample1\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\t\tunknown\t\t${furtherValues}\n" +
                        "sample3\t${bulkSeqTypeWithAntibodyTarget.name}\t${bulkSeqTypeWithAntibodyTarget.libraryLayout.name()}\t\t\tunknown\t${furtherValues}\n" +
                        "${project.name}#${scSample.individual.pid}#${scSample.sampleType.name}\t${scSeqType.name}\t${scSeqType.libraryLayout.name()}\tunknown\t${SeqType.SINGLE_CELL_DNA}\t\t${furtherValues}\n"
        )

        Validator<MetadataValidationContext> validator = new MergingPreventionValidator([
                antibodyTargetService       : new AntibodyTargetService(),
                libraryPreparationKitService: new LibraryPreparationKitService(),
                metadataImportService       : new MetadataImportService([
                        seqTypeService: new SeqTypeService(),
                ]),
                sampleIdentifierService     : Spy(SampleIdentifierService) {
                    _ * getSampleIdentifierParser(_) >> { SampleIdentifierParserBeanName sampleIdentifierParserBeanName ->
                        new TestSampleIdentifierParser()
                    }
                },
                seqPlatformService          : new SeqPlatformService([
                        seqPlatformModelLabelService: new SeqPlatformModelLabelService(),
                        sequencingKitLabelService   : new SequencingKitLabelService()
                ]),
                seqTypeService              : new SeqTypeService(),
        ])

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "Sample ${scSample.displayName} with sequencing type ${scSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.WARNING,
                        "Sample ${bulkSample.displayName} with sequencing type ${bulkSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.WARNING,
                        "Sample ${bulkSample.displayName} with sequencing type ${bulkSeqTypeWithAntibodyTarget.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, LogLevel.ERROR,
                        "Sample ${scSample.displayName} with sequencing type ${scSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, LogLevel.WARNING,
                        "Sample ${bulkSample.displayName} with sequencing type ${bulkSeqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
        ]
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    @Unroll
    void "validate, if compatible mergingWorkPackage exist and is not withdrawn, add merging warning (case: #name)"() {
        given:
        Map data = createData(factory, seqTypeClosure)

        SeqPlatformGroup seqPlatformGroup = data.mergingWorkPackage.seqPlatformGroup
        seqPlatformGroup.seqPlatforms.add(data.seqPlatform)
        seqPlatformGroup.save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(data.content)

        when:
        data.validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, errorLevel,
                        "Sample ${data.mergingWorkPackage.sample.displayName} with sequencing type " +
                                "${data.mergingWorkPackage.seqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples."),
        ]
        TestCase.assertContainSame(expectedProblems, context.problems)

        where:
        input << CASE_SAMPLE_EXIST_CAUSE_MESSAGE

        name = input.name
        factory = input.factory
        seqTypeClosure = input.seqTypeClosure
        errorLevel = input.errorLevel
    }

    @Unroll
    void "validate, if compatible mergingWorkPackage exist, but has only withdrawn data, do not create warning (case: #name)"() {
        given:
        Map data = createData(factory, seqTypeClosure, true)

        SeqPlatformGroup seqPlatformGroup = data.mergingWorkPackage.seqPlatformGroup
        seqPlatformGroup.seqPlatforms.add(data.seqPlatform)
        seqPlatformGroup.save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(data.content)

        when:
        data.validator.validate(context)

        then:
        context.problems.empty

        where:
        input << CASE_SAMPLE_EXIST_CAUSE_MESSAGE

        name = input.name
        factory = input.factory
        seqTypeClosure = input.seqTypeClosure
        errorLevel = input.errorLevel
    }

    @Unroll
    void "validate, if mergingWorkPackage has not compatible seqPlatform, add merging warning (case: #name, withdrawn: #withdrawn)"() {
        given:
        Map data = createData(factory, seqTypeClosure, withdrawn)

        SeqPlatformGroup seqPlatformGroup = data.mergingWorkPackage.seqPlatformGroup
        seqPlatformGroup.seqPlatforms.add(createSeqPlatform())
        seqPlatformGroup.save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(data.content)

        when:
        data.validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, errorLevel,
                        "Sample ${data.mergingWorkPackage.sample.displayName} with sequencing type " +
                                "${data.mergingWorkPackage.seqType.displayNameWithLibraryLayout} can not be merged with the existing bam file, since " +
                                "new seq platform ${data.seqPlatform} is not compatible with seq platform group ${data.mergingWorkPackage.seqPlatformGroup}",
                        "Sample can not be merged with existing data, because merging criteria is incompatible."),
        ]
        TestCase.assertContainSame(expectedProblems, context.problems)

        where:
        input << CASE_SEQ_PLATFORM_DIFFER_CAUSE_MESSAGE.collectMany {
            [
                    [withdrawn: true] + it,
                    [withdrawn: false] + it,
            ]
        }

        name = input.name
        factory = input.factory
        seqTypeClosure = input.seqTypeClosure
        errorLevel = input.errorLevel
        withdrawn = input.withdrawn
    }

    @Unroll
    void "validate, if libraryPreparationKit is used and different, add merging warning (case: #name, withdrawn: #withdrawn)"() {
        given:
        Map data = createData(factory, seqTypeClosure, withdrawn)

        SeqPlatformGroup seqPlatformGroup = data.mergingWorkPackage.seqPlatformGroup
        seqPlatformGroup.seqPlatforms.add(data.seqPlatform)
        seqPlatformGroup.save(flush: true)
        data.mergingWorkPackage.libraryPreparationKit = createLibraryPreparationKit()
        data.mergingWorkPackage.save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(data.content)

        when:
        data.validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, errorLevel,
                        "Sample ${data.mergingWorkPackage.sample.displayName} with sequencing type " +
                                "${data.mergingWorkPackage.seqType.displayNameWithLibraryLayout} can not be merged with the existing bam file, since " +
                                "new library preparation kit ${data.libraryPreparationKit} differs from old library preparation kit ${data.mergingWorkPackage.libraryPreparationKit}",
                        "Sample can not be merged with existing data, because merging criteria is incompatible."),
        ]
        TestCase.assertContainSame(expectedProblems, context.problems)

        where:
        input << CASE_LIBRARY_PREPARATION_KIT_DIFFER_CAUSE_MESSAGE.collectMany {
            [
                    [withdrawn: true] + it,
                    [withdrawn: false] + it,
            ]
        }

        name = input.name
        factory = input.factory
        seqTypeClosure = input.seqTypeClosure
        errorLevel = input.errorLevel
        withdrawn = input.withdrawn
    }

    static private final List<Map<String, ?>> CASE_LIBRARY_PREPARATION_KIT_DIFFER_CAUSE_MESSAGE = [
            [
                    name          : 'pancan wgs paired',
                    factory       : AlignmentPipelineFactory.RoddyPancanFactoryInstance.INSTANCE,
                    seqTypeClosure: { DomainFactory.createWholeGenomeSeqType() },
                    errorLevel    : LogLevel.WARNING,
            ],
            [
                    name          : 'pancan wes paired',
                    factory       : AlignmentPipelineFactory.RoddyPancanFactoryInstance.INSTANCE,
                    seqTypeClosure: { DomainFactory.createExomeSeqType() },
                    errorLevel    : LogLevel.WARNING,
            ],
            [
                    name          : 'pancan chipseq paired',
                    factory       : AlignmentPipelineFactory.RoddyPancanFactoryInstance.INSTANCE,
                    seqTypeClosure: { DomainFactory.createChipSeqType() },
                    errorLevel    : LogLevel.WARNING,
            ],
            [
                    name          : 'pancan rna paired',
                    factory       : AlignmentPipelineFactory.RoddyRnaFactoryInstance.INSTANCE,
                    seqTypeClosure: { DomainFactory.createRnaPairedSeqType() },
                    errorLevel    : LogLevel.WARNING,
            ],
            [
                    name          : 'pancan rna single',
                    factory       : AlignmentPipelineFactory.RoddyRnaFactoryInstance.INSTANCE,
                    seqTypeClosure: { DomainFactory.createRnaSingleSeqType() },
                    errorLevel    : LogLevel.WARNING,
            ],
    ]*.asImmutable().asImmutable()

    static private final List<Map<String, ?>> CASE_SEQ_PLATFORM_DIFFER_CAUSE_MESSAGE = CASE_LIBRARY_PREPARATION_KIT_DIFFER_CAUSE_MESSAGE + [
            [
                    name          : 'pancan wgbs paired',
                    factory       : AlignmentPipelineFactory.RoddyPancanFactoryInstance.INSTANCE,
                    seqTypeClosure: { DomainFactory.createWholeGenomeBisulfiteSeqType() },
                    errorLevel    : LogLevel.WARNING,
            ],
            [
                    name          : 'pancan wgbstag paired',
                    factory       : AlignmentPipelineFactory.RoddyPancanFactoryInstance.INSTANCE,
                    seqTypeClosure: { DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType() },
                    errorLevel    : LogLevel.WARNING,
            ],
    ]*.asImmutable().asImmutable()

    static private final List<Map<String, ?>> CASE_SAMPLE_EXIST_CAUSE_MESSAGE = CASE_SEQ_PLATFORM_DIFFER_CAUSE_MESSAGE + [
            [
                    name          : 'cellranger paired',
                    factory       : AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE,
                    seqTypeClosure: { AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createSeqType() },
                    errorLevel    : LogLevel.ERROR,
            ],
    ]*.asImmutable().asImmutable()

    private Map<String, ?> createData(IsAlignment alignment, Closure<SeqType> seqTypeClosure, boolean withdrawnSeqTracks = false) {
        DomainFactory.createAllAlignableSeqTypes()

        MergingWorkPackage mergingWorkPackage = alignment.createMergingWorkPackage([
                seqType: seqTypeClosure(),
        ])
        mergingWorkPackage.project.alignmentDeciderBeanName = OTP_ALIGNMENT
        mergingWorkPackage.project.save(flush: true)

        createMergingCriteria([
                project: mergingWorkPackage.project,
                seqType: mergingWorkPackage.seqType,
        ])

        addSeqTrack(mergingWorkPackage, withdrawnSeqTracks)

        SeqPlatform seqPlatform = createSeqPlatform()

        SampleIdentifier sampleIdentifier = createSampleIdentifier([
                sample: mergingWorkPackage.sample,
        ])

        String content = [
                (PROJECT)             : mergingWorkPackage.project.name,
                (SAMPLE_NAME)         : sampleIdentifier.name,
                (SEQUENCING_TYPE)     : mergingWorkPackage.seqType.name,
                (SEQUENCING_READ_TYPE): mergingWorkPackage.seqType.libraryLayout,
                (BASE_MATERIAL)       : mergingWorkPackage.seqType.singleCell ? SeqType.SINGLE_CELL_DNA : '',
                (ANTIBODY_TARGET)     : mergingWorkPackage.antibodyTarget?.name ?: '',
                (INSTRUMENT_PLATFORM) : seqPlatform.name,
                (INSTRUMENT_MODEL)    : seqPlatform.seqPlatformModelLabel.name,
                (SEQUENCING_KIT)      : seqPlatform.sequencingKitLabel?.name ?: '',
                (LIB_PREP_KIT)        : mergingWorkPackage.libraryPreparationKit,
        ].collect { key, value ->
            [key, value]
        }.transpose()*.join('\t').join('\n')

        Validator<MetadataValidationContext> validator = new MergingPreventionValidator([
                antibodyTargetService       : new AntibodyTargetService(),
                libraryPreparationKitService: new LibraryPreparationKitService(),
                metadataImportService       : new MetadataImportService([
                        seqTypeService: new SeqTypeService(),
                ]),
                sampleIdentifierService     : new SampleIdentifierService(),
                seqTypeService              : new SeqTypeService(),
                seqPlatformService          : new SeqPlatformService([
                        seqPlatformModelLabelService: new SeqPlatformModelLabelService(),
                        sequencingKitLabelService   : new SequencingKitLabelService()
                ]),
        ])

        return [
                mergingWorkPackage   : mergingWorkPackage,
                seqPlatform          : seqPlatform,
                libraryPreparationKit: mergingWorkPackage.libraryPreparationKit,
                content              : content,
                validator            : validator,
        ]
    }

    private void addSeqTrack(MergingWorkPackage mergingWorkPackage, boolean withdrawnSeqTracks = false) {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(mergingWorkPackage)
        createDataFile([
                seqTrack     : seqTrack,
                fileWithdrawn: withdrawnSeqTracks,
        ])
        mergingWorkPackage.addToSeqTracks(seqTrack)
        mergingWorkPackage.save(flush: true)
    }
}
