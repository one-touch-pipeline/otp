package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.INDIVIDUAL
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock([Individual, Realm, Project])
class IndividualValidatorSpec extends Specification {

    void 'validate, when column INDIVIDUAL missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                        "SomeValue"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column '${INDIVIDUAL}' is missing.")
        ]

        when:
        new IndividualValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when column exist and individual is registered in OTP, succeeds'() {

        given:
        String INDIVIDUAL_PID = "individualPid"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${INDIVIDUAL}\n" +
                        "${INDIVIDUAL_PID}\n"
        )

        DomainFactory.createIndividual([pid: INDIVIDUAL_PID])

        when:
        new IndividualValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist but individual is not registered in OTP, adds problems'() {

        given:
        String INDIVIDUAL_PID = "individualPid"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${INDIVIDUAL}\n" +
                        "${INDIVIDUAL_PID}\n"
        )

        when:
        new IndividualValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The individual '${INDIVIDUAL_PID}' is not registered in OTP.")
    }
}