package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import grails.test.mixin.Mock
import spock.lang.Specification

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement


@Mock([
        LibraryPreparationKit,
        SeqType,
])
class LibPrepKitSeqTypeValidatorSpec extends Specification {

    static final String VALID_METADATA =
            "${MetaDataColumn.LIB_PREP_KIT}\t${MetaDataColumn.SEQUENCING_TYPE}\n" +
                    "lib_prep_kit\t${SeqTypeNames.EXOME.seqTypeName}\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${SeqTypeNames.EXOME.seqTypeName}\n"

    void setup() {
        DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit')
        DomainFactory.createExomeSeqType()
    }


    void 'validate, when sequencing type is Exome and LibPrepKit is valid'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when sequencing type is Exome and LibPrepKit is empty, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "\t${SeqTypeNames.EXOME.seqTypeName}\n"
        )

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A4', 'B4'])
        problem.message.contains("If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}', the library preparation kit must not be empty.")
    }

    void 'validate, when sequencing type is not exome'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "\t${SeqTypeNames.WHOLE_GENOME.seqTypeName}\n"
        )

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }
}
