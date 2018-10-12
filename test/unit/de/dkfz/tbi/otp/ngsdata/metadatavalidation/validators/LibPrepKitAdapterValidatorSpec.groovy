package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import java.util.regex.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*


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
        validator.sampleIdentifierService = [
                parseSampleIdentifier: { String sampleId ->
                    Matcher match = sampleId =~ /sample(\d+)/
                    if (match.matches()) {
                        return new DefaultParsedSampleIdentifier("project${match.group(1)}", sampleId, sampleId, sampleId)
                    } else {
                        return null
                    }
                }
        ] as SampleIdentifierService
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.exomePairedSeqType
            9 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.rnaPairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false])  >> SeqTypeService.chipSeqPairedSeqType
        }

        LibraryPreparationKit kitWithoutAdapterFileAndSequence = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file_and_sequence')
        LibraryPreparationKit kitWithoutAdapterFile = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file', reverseComplementAdapterSequence: "ACGTC")
        LibraryPreparationKit kitWithoutAdapterSequence = DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_sequence', adapterFile: "/asdf")

        SampleIdentifier normalIdentifier = DomainFactory.createSampleIdentifier()

        SampleIdentifier identifierWithIndividualRoddyConfig = DomainFactory.createSampleIdentifier()
        identifierWithIndividualRoddyConfig.sample.individual.project.alignmentDeciderBeanName = AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean
        identifierWithIndividualRoddyConfig.sample.individual.project.save(flush: true)
        DomainFactory.createRoddyWorkflowConfig([
                individual: identifierWithIndividualRoddyConfig.individual,
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                adapterTrimmingNeeded: true,
        ])

        SampleIdentifier identifierWithProjectRoddyConfig = DomainFactory.createSampleIdentifier()
        identifierWithProjectRoddyConfig.sample.individual.project.alignmentDeciderBeanName = AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean
        identifierWithProjectRoddyConfig.sample.individual.project.save(flush: true)
        DomainFactory.createRoddyWorkflowConfig([
                project: identifierWithProjectRoddyConfig.project,
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                adapterTrimmingNeeded: true,
        ])

        SampleIdentifier identifierRnaWithProjectRoddyConfig = DomainFactory.createSampleIdentifier()
        identifierRnaWithProjectRoddyConfig.sample.individual.project.alignmentDeciderBeanName = AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean
        identifierRnaWithProjectRoddyConfig.sample.individual.project.save(flush: true)
        DomainFactory.createRoddyWorkflowConfig([
                project: identifierRnaWithProjectRoddyConfig.project,
                seqType: DomainFactory.createRnaPairedSeqType(),
                pipeline: DomainFactory.createRnaPipeline(),
                adapterTrimmingNeeded: true,
        ])

        DomainFactory.createRoddyWorkflowConfig([
                project: DomainFactory.createProject(name: "project01", alignmentDeciderBeanName: AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean),
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                adapterTrimmingNeeded: true,
        ])

        SampleIdentifier identifierWithOtpAlignment = DomainFactory.createSampleIdentifier()
        identifierWithOtpAlignment.sample.individual.project.alignmentDeciderBeanName = AlignmentDeciderBeanNames.OTP_ALIGNMENT.bean
        identifierWithOtpAlignment.sample.individual.project.save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\t${LIB_PREP_KIT.name()}\t${SAMPLE_ID.name()}\t${LIBRARY_LAYOUT.name()}\n" +
                        "${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${normalIdentifier}\t${LibraryLayout.PAIRED}\n" +
                        "${SeqTypeNames.EXOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${normalIdentifier}\t${LibraryLayout.PAIRED}\n" +
                        // project config -> adapter file missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\t${identifierWithProjectRoddyConfig}\t${LibraryLayout.PAIRED}\n" +
                        // individual config -> adapter file missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\t${identifierWithIndividualRoddyConfig}\t${LibraryLayout.PAIRED}\n" +
                        // RNA project config -> adapter sequence missing
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterSequence.name}\t${identifierRnaWithProjectRoddyConfig}\t${LibraryLayout.PAIRED}\n" +
                        // parsed sample identifier -> adapter file missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\tsample01\t${LibraryLayout.PAIRED}\n" +
                        // parse sample identifier with unknown project
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\tsample02\t${LibraryLayout.PAIRED}\n" +
                        // using OTP alignment
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${identifierWithOtpAlignment}\t${LibraryLayout.PAIRED}\n" +
                        // unknown lib prep kit
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\tunknown_kit\t${identifierWithProjectRoddyConfig}\t${LibraryLayout.PAIRED}\n" +
                        // empty lib prep kit
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t\t${identifierWithProjectRoddyConfig}\t${LibraryLayout.PAIRED}\n" +
                        // unknown sample identifier
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\tunknown_sample\t${LibraryLayout.PAIRED}\n" +
                        // empty sample identifier
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t\t${LibraryLayout.PAIRED}\n" +
                        // unknown seq type
                        "WHOLE_UNKNOWN_SEQUENCING\t${kitWithoutAdapterFileAndSequence.name}\t${identifierWithProjectRoddyConfig}\t${LibraryLayout.PAIRED}\n" +
                        // empty seq type
                        "\t${kitWithoutAdapterFileAndSequence.name}\t${identifierWithProjectRoddyConfig}\t${LibraryLayout.PAIRED}\n" +
                        // non roddy seq type
                        "${SeqTypeNames.CHIP_SEQ.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${identifierWithProjectRoddyConfig}\t${LibraryLayout.PAIRED}\n"
        )

        when:
        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[2].cells) as Set, Level.WARNING,
                        "Adapter trimming is requested but adapter file for library preparation kit '${kitWithoutAdapterFile}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing."),
                new Problem((context.spreadsheet.dataRows[3].cells) as Set, Level.WARNING,
                        "Adapter trimming is requested but adapter file for library preparation kit '${kitWithoutAdapterFile}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing."),
                new Problem((context.spreadsheet.dataRows[4].cells) as Set, Level.WARNING,
                        "Adapter trimming is requested but reverse complement adapter sequence for library preparation kit '${kitWithoutAdapterSequence}' is missing.", "Adapter trimming is requested but the reverse complement adapter sequence for at least one library preparation kit is missing."),
                new Problem((context.spreadsheet.dataRows[5].cells) as Set, Level.WARNING,
                        "Adapter trimming is requested but adapter file for library preparation kit '${kitWithoutAdapterFile}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing."),
        ]
        containSame(context.problems, expectedProblems)
    }
}
