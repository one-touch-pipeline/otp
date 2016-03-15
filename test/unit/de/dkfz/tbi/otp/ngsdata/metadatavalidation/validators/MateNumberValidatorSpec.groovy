package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import spock.lang.Specification

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class MateNumberValidatorSpec extends Specification {

    void 'validate, when column is missing, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        when:
        new MateNumberValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Mandatory column 'MATE' is missing.")
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
                        "The mate number ('-1') must be a positive integer (value >= 1)."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The mate number ('0') must be a positive integer (value >= 1)."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The mate number ('abc') must be a positive integer (value >= 1)."),
        ]

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}
