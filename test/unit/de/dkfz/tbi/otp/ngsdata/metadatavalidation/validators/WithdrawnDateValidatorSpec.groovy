package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class WithdrawnDateValidatorSpec extends Specification {

    void 'validate adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.WITHDRAWN_DATE.name()}\n" +
                        "2015\n" +
                        "\n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Withdrawn data cannot be imported into OTP."),
        ]

        when:
        new WithdrawnDateValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }


    void 'validate, when metadata does not contain a column WITHDRAWN_DATE, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new WithdrawnDateValidator().validate(context)

        then:
        context.problems.empty
    }
}
