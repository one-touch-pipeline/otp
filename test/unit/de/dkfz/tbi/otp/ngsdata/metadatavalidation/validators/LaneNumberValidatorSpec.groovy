package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame

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
                        "'0' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.WARNING,
                        "'9' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.WARNING,
                        "'001' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.WARNING,
                        "'1a' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The lane number must not be empty."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "'1_ABC' is not a well-formed lane number. It must contain only digits (0 to 9) and/or letters (a to z, A to Z). It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed."),
        ]

        when:
        new LaneNumberValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
