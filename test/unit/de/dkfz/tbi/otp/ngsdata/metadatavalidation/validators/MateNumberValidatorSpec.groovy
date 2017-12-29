package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

class MateNumberValidatorSpec extends Specification {

    void 'validate, when column is missing, adds no error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        when:
        new MateNumberValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, all are fine'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MATE}
1
2
""")

        when:
        new MateNumberValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MATE}

-1
0
1
2
3
4
abc
""")

        when:
        new MateNumberValidator().validate(context)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The mate number must be provided and must be a positive integer (value >= 1)."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The mate number ('-1') must be a positive integer (value >= 1).", "At least one mate number is not a positive integer number."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The mate number ('0') must be a positive integer (value >= 1).", "At least one mate number is not a positive integer number."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The mate number ('abc') must be a positive integer (value >= 1).", "At least one mate number is not a positive integer number."),
        ]

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
