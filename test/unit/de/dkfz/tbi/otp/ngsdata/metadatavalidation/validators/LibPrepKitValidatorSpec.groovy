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
])
class LibPrepKitValidatorSpec extends Specification {

    LibPrepKitValidator validator

    static final String VALID_METADATA =
            "${MetaDataColumn.LIB_PREP_KIT}\n" +
                    "lib_prep_kit\n" +
                    "\n" +
                    "lib_prep_kit_import_alias\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\n"

    void setup() {
        validator = new LibPrepKitValidator()
        validator.libraryPreparationKitService = new LibraryPreparationKitService()
        DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit', importAlias: ["lib_prep_kit_import_alias"])
    }


    void 'validate, when metadata file contains valid LibPrepKit'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when metadata file contains invalid LibPrepKit, adds error'() {

        given:
        String invalid = "totally_invalid_lib_prep_kit"
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA + "${invalid}\n"
        )

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A6'])
        problem.message.contains("The library preparation kit '${invalid}' is neither registered in the OTP database nor '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' nor empty.")
    }

    void 'validate, when metadata file contains no LibPrepKit, succeeds without problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.LIB_PREP_KIT}\tother
\tabc
\tdef
"""
        )

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when metadata file contains no LibPrepKit column, adds one error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Optional column 'LIB_PREP_KIT' is missing.")
    }
}
