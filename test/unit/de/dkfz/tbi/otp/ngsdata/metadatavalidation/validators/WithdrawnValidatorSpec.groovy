package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class WithdrawnValidatorSpec extends Specification {

    void 'validate adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.WITHDRAWN}\n" +
                        "YES\n" +
                        "TRUE\n" +
                        "1\n" +
                        "-1\n" +
                        "\n" +
                        "0\n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Withdrawn data cannot be imported into OTP."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "Withdrawn data cannot be imported into OTP."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "Withdrawn data cannot be imported into OTP."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "Withdrawn data cannot be imported into OTP."),
        ]

        when:
        new WithdrawnValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }


    void 'validate, when metadata does not contain a column WITHDRAWN, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new WithdrawnValidator().validate(context)

        then:
        context.problems.empty
    }
}
