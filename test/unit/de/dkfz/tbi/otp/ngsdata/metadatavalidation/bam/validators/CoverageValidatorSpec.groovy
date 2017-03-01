package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class CoverageValidatorSpec extends Specification {

    void 'validate, when column COVERAGE missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext()
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "Optional column '${COVERAGE}' is missing.")
        ]

        when:
        new CoverageValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate context with errors'() {

        given:
        String COVERAGE_NO_DOUBLE = "cov123"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${COVERAGE}\n" +
                        "${COVERAGE_NO_DOUBLE}\n" +
                        "\n" +
                        "10.56565\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The coverage '${COVERAGE_NO_DOUBLE}' should be a double number."),
        ]


        when:
        new CoverageValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}