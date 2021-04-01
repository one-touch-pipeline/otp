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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class LibPrepKitAdapterValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                LibraryPreparationKit,
                Pipeline,
                ProcessingOption,
                Project,
                Realm,
                RoddyWorkflowConfig,
                Sample,
                SampleIdentifier,
                SampleType,
                SeqType,
        ]
    }

    void 'validate, when metadata file contains valid data, succeeds'() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createPanCanPipeline()
        DomainFactory.createRnaPipeline()

        LibPrepKitAdapterValidator validator = new LibPrepKitAdapterValidator()
        validator.libraryPreparationKitService = new LibraryPreparationKitService()

        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false])  >> SeqTypeService.exomePairedSeqType
            5 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false])  >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false])  >> SeqTypeService.rnaPairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false])  >> SeqTypeService.chipSeqPairedSeqType
            1 * findByNameOrImportAlias('WHOLE_UNKNOWN_SEQUENCING', [libraryLayout: SequencingReadType.PAIRED, singleCell: false])  >> null
        }

        LibraryPreparationKit kitWithoutAdapterFileAndSequence = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file_and_sequence')
        LibraryPreparationKit kitWithoutAdapterFile = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file', reverseComplementAdapterSequence: "ACGTC")
        LibraryPreparationKit kitWithoutAdapterSequence = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_sequence', adapterFile: "/asdf")

        Project project1 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT)
        Project project2 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT)
        Project project3 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT)
        Project project4 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT)
        Project project5 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.OTP_ALIGNMENT)

        DomainFactory.createRoddyWorkflowConfig([
                individual: DomainFactory.createIndividual(project: project2),
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                adapterTrimmingNeeded: true,
        ])

        DomainFactory.createRoddyWorkflowConfig([
                project: project3,
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                adapterTrimmingNeeded: true,
        ])

        DomainFactory.createRoddyWorkflowConfig([
                project: project4,
                seqType: DomainFactory.createRnaPairedSeqType(),
                pipeline: DomainFactory.createRnaPipeline(),
                adapterTrimmingNeeded: true,
        ])

        DomainFactory.createRoddyWorkflowConfig([
                project: DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT),
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                adapterTrimmingNeeded: true,
        ])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\t${LIB_PREP_KIT.name()}\t${PROJECT.name()}\t${SEQUENCING_READ_TYPE.name()}\n" +
                        "${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project1.name}\t${SequencingReadType.PAIRED}\n" +
                        "${SeqTypeNames.EXOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project1.name}\t${SequencingReadType.PAIRED}\n" +
                        // project config -> adapter file missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\t${project2.name}\t${SequencingReadType.PAIRED}\n" +
                        // individual config -> adapter file missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\t${project3.name}\t${SequencingReadType.PAIRED}\n" +
                        // RNA project config -> adapter sequence missing
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterSequence.name}\t${project4.name}\t${SequencingReadType.PAIRED}\n" +
                        // using OTP alignment
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project5.name}\t${SequencingReadType.PAIRED}\n" +
                        // unknown lib prep kit
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tunknown_kit\t${project3.name}\t${SequencingReadType.PAIRED}\n" +
                        // empty lib prep kit
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t\t${project3.name}\t${SequencingReadType.PAIRED}\n" +
                        // unknown seq type
                        "WHOLE_UNKNOWN_SEQUENCING\t${kitWithoutAdapterFileAndSequence.name}\t${project3.name}\t${SequencingReadType.PAIRED}\n" +
                        // empty seq type
                        "\t${kitWithoutAdapterFileAndSequence.name}\t${project3.name}\t${SequencingReadType.PAIRED}\n" +
                        // non roddy seq type
                        "${SeqTypeNames.CHIP_SEQ.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project3.name}\t${SequencingReadType.PAIRED}\n"
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[3].cells) as Set, Level.WARNING,
                        "Adapter trimming is requested but adapter file for library preparation kit '${kitWithoutAdapterFile}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing."),
                new Problem((context.spreadsheet.dataRows[4].cells) as Set, Level.WARNING,
                        "Adapter trimming is requested but reverse complement adapter sequence for library preparation kit '${kitWithoutAdapterSequence}' is missing.", "Adapter trimming is requested but the reverse complement adapter sequence for at least one library preparation kit is missing."),
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}
