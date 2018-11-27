package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Mock([
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
])
class LibPrepKitAdapterValidatorSpec extends Specification {

    void 'validate, when metadata file contains valid data, succeeds'() {

        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createPanCanPipeline()
        DomainFactory.createRnaPipeline()

        LibPrepKitAdapterValidator validator = new LibPrepKitAdapterValidator()
        validator.libraryPreparationKitService = new LibraryPreparationKitService()

        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.exomePairedSeqType
            5 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.rnaPairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.chipSeqPairedSeqType
            1 * findByNameOrImportAlias('WHOLE_UNKNOWN_SEQUENCING', [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> null
        }

        LibraryPreparationKit kitWithoutAdapterFileAndSequence = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file_and_sequence')
        LibraryPreparationKit kitWithoutAdapterFile = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file', reverseComplementAdapterSequence: "ACGTC")
        LibraryPreparationKit kitWithoutAdapterSequence = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_sequence', adapterFile: "/asdf")

        Project project1 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName)
        Project project2 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName)
        Project project3 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName)
        Project project4 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName)
        Project project5 = DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.OTP_ALIGNMENT.beanName)

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
                project: DomainFactory.createProject(alignmentDeciderBeanName: AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName),
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                adapterTrimmingNeeded: true,
        ])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\t${LIB_PREP_KIT.name()}\t${PROJECT.name()}\t${LIBRARY_LAYOUT.name()}\n" +
                        "${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project1.name}\t${LibraryLayout.PAIRED}\n" +
                        "${SeqTypeNames.EXOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project1.name}\t${LibraryLayout.PAIRED}\n" +
                        // project config -> adapter file missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\t${project2.name}\t${LibraryLayout.PAIRED}\n" +
                        // individual config -> adapter file missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\t${project3.name}\t${LibraryLayout.PAIRED}\n" +
                        // RNA project config -> adapter sequence missing
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterSequence.name}\t${project4.name}\t${LibraryLayout.PAIRED}\n" +
                        // using OTP alignment
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project5.name}\t${LibraryLayout.PAIRED}\n" +
                        // unknown lib prep kit
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tunknown_kit\t${project3.name}\t${LibraryLayout.PAIRED}\n" +
                        // empty lib prep kit
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t\t${project3.name}\t${LibraryLayout.PAIRED}\n" +
                        // unknown seq type
                        "WHOLE_UNKNOWN_SEQUENCING\t${kitWithoutAdapterFileAndSequence.name}\t${project3.name}\t${LibraryLayout.PAIRED}\n" +
                        // empty seq type
                        "\t${kitWithoutAdapterFileAndSequence.name}\t${project3.name}\t${LibraryLayout.PAIRED}\n" +
                        // non roddy seq type
                        "${SeqTypeNames.CHIP_SEQ.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${project3.name}\t${LibraryLayout.PAIRED}\n"
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
