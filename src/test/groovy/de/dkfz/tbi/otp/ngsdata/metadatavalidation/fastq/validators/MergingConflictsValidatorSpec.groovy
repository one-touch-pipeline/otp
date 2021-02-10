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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.parser.TestSampleIdentifierParser
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MergingConflictsValidatorSpec extends Specification implements DataTest, DomainFactoryCore, AlignmentPipelineFactory {

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

    void "validate"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createCellRangerAlignableSeqTypes()

        Project project = createProject(
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.DEEP,
        )

        Sample sample1 = createSample()
        sample1.individual.project = project
        sample1.individual.save(flush: true)
        SeqType seqType1 = DomainFactory.proxyCellRanger.createSeqType()
        DomainFactory.createSampleIdentifier(sample: sample1, name: "sample1")

        Sample sample2 = createSample()
        sample2.individual.project = project
        sample2.individual.save(flush: true)
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

        Validator<MetadataValidationContext> validator = new MergingConflictsValidator([
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
                new Problem((context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[3].cells + context.spreadsheet.dataRows[4].cells) as Set, Level.WARNING,
                        "Sample ${sample2.individual.pid} ${sample2.sampleType.displayName} with sequencing type ${seqType2.displayNameWithLibraryLayout} cannot be merged with itself, since it uses incompatible seq platforms",
                        "Sample can not be merged with itself, since it uses incompatible seq platforms."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set, Level.WARNING,
                        "Sample ${individual3} ${sampleType3} with sequencing type ${seqType2.displayNameWithLibraryLayout} cannot be merged with itself, since it uses incompatible seq platforms",
                        "Sample can not be merged with itself, since it uses incompatible seq platforms."),
        ]
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
