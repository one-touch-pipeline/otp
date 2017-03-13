package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        LibraryPreparationKit,
        SeqType,
])
class LibPrepKitSeqTypeValidatorSpec extends Specification {

    static final String VALID_METADATA =
            "${MetaDataColumn.LIB_PREP_KIT}\t${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\n" +
                    "lib_prep_kit\t${SeqTypeNames.EXOME.seqTypeName}\t\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${SeqTypeNames.EXOME.seqTypeName}\t\n" +
                    "lib_prep_kit\t${SeqTypeNames.RNA.seqTypeName}\t\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${SeqTypeNames.RNA.seqTypeName}\t\n"

    void setup() {
        DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit')
        DomainFactory.createExomeSeqType()
    }


    void 'validate, when sequencing type is exome or RNA and LibPrepKit is valid'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when sequencing type is exome or RNA and LibPrepKit is empty, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "\t${SeqTypeNames.EXOME.seqTypeName}\t\n" +
                        "\t${SeqTypeNames.RNA.seqTypeName}\t\n"
        )

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.size() == 2
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[4].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' or '${SeqTypeNames.RNA.seqTypeName}', the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[5].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' or '${SeqTypeNames.RNA.seqTypeName}', the library preparation kit must be given."),
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing type is not exome'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "\t${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t\n" +
                        "\t${SeqTypeNames.EXOME.seqTypeName}\ttrue\n"
        )

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when sequencing type is not exome and no libPrepKit column exist, succeeds without problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}
WGS
WGBS
CHIPSEQ
""")

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when sequencing type is exome or RNA and no libPrepKit column exist, adds one error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}
WGS
WGBS
${SeqTypeNames.EXOME.seqTypeName}
WGS
WGBS
${SeqTypeNames.EXOME.seqTypeName}
${SeqTypeNames.RNA.seqTypeName}
""")

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.size() == 2
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[5].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' or '${SeqTypeNames.RNA.seqTypeName}', the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[6].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' or '${SeqTypeNames.RNA.seqTypeName}', the library preparation kit must be given."),
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing type column missing, succeeds without problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }
}
