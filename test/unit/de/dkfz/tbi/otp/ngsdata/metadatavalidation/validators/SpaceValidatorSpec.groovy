package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.Specification

class SpaceValidatorSpec extends Specification {

    void 'validate adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                " x\n" +
                "x \n" +
                "x  x\n" +
                "  \n" +
                "x\n" +
                "x\n" +
                " x\n" +
                "x \n" +
                "x  x\n" +
                "  \n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.header.cells + context.spreadsheet.dataRows[5].cells as Set, Level.WARNING,
                        "' x' starts with a space character."),
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[6].cells as Set, Level.WARNING,
                        "'x ' ends with a space character."),
                new Problem(context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[7].cells as Set, Level.WARNING,
                        "'x  x' contains subsequent space characters."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[8].cells as Set, Level.WARNING,
                        "'  ' starts with a space character."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[8].cells as Set, Level.WARNING,
                        "'  ' ends with a space character."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[8].cells as Set, Level.WARNING,
                        "'  ' contains subsequent space characters."),
        ]

        when:
        new SpaceValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
