package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*

class LaneNumberValidatorSpec extends Specification {

    void 'validate adds expected error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.LANE_NO}\n" +
                        "0\n" +
                        "1\n" +
                        "8\n" +
                        "9\n" +
                        "001\n" +
                        "1a\n" +
                        "\n" +
                        "1_ABC")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "'0' is not a well-formed lane number. It should be a single digit in the range from 1 to 8."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.WARNING,
                        "'9' is not a well-formed lane number. It should be a single digit in the range from 1 to 8."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.WARNING,
                        "'001' is not a well-formed lane number. It should be a single digit in the range from 1 to 8."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.WARNING,
                        "'1a' is not a well-formed lane number. It should be a single digit in the range from 1 to 8."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The lane number must not be empty."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "'1_ABC' is not a well-formed lane number. It must contain only digits (0 to 9) and/or letters (a to z, A to Z). It should be a single digit in the range from 1 to 8."),
        ]

        when:
        new LaneNumberValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
