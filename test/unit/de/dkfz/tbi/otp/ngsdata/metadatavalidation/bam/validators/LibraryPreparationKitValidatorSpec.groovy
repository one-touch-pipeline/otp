package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.LIBRARY_PREPARATION_KIT
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

@Mock([LibraryPreparationKit])
class LibraryPreparationKitValidatorSpec extends Specification {

    void 'validate, when library preparation kit is not registered in OTP, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE}\t${LIBRARY_PREPARATION_KIT}
EXON\tIndividual1
""")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The ${LIBRARY_PREPARATION_KIT} 'Individual1' is not registered in OTP.",
                        "At least one ${LIBRARY_PREPARATION_KIT} is not registered in OTP.")
        ]

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when library preparation kit is registered in OTP and is EXOME, succeeds'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE}\t${LIBRARY_PREPARATION_KIT}
EXON\tIndividual1\t
""")

        DomainFactory.createLibraryPreparationKit([name: "Individual1"])

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, without library preparation kit but EXOME, adds problems'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\n" +
                        "EXON\n"
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "The ${SEQUENCING_TYPE} is 'EXON' but no ${LIBRARY_PREPARATION_KIT} is given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.",
                        "If the ${SEQUENCING_TYPE} is '${SeqTypeNames.EXOME.seqTypeName}' the ${LIBRARY_PREPARATION_KIT} should be given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.")
        ]

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, without library preparation kit and without EXOME, succeeds'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\n" +
                        "seqType\n"
        )

        when:
        new LibraryPreparationKitValidator().validate(context)

        then:
        context.problems.empty
    }
}
