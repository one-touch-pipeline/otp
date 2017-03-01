package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([ReferenceGenome])
class ReferenceGenomeValidatorSpec extends Specification {

    void 'validate, when column REFERENCE_GENOME missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "SomeColumn\n"+
                        "SomeValue"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column '${REFERENCE_GENOME}' is missing.")
        ]

        when:
        new ReferenceGenomeValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when column exist and referenceGenome is registered in OTP, succeeds'() {

        given:
        String REF_GEN_NAME = "refGenName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${REFERENCE_GENOME}\n" +
                        "${REF_GEN_NAME}\n"
        )

        DomainFactory.createReferenceGenome([name: REF_GEN_NAME])

        when:
        new ReferenceGenomeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist but referenceGenome is not registered in OTP, adds problems'() {

        given:
        String REF_GEN_NAME = "refGenName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${REFERENCE_GENOME}\n" +
                        "${REF_GEN_NAME}\n"
        )

        when:
        new ReferenceGenomeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The reference genome '${REF_GEN_NAME}' is not registered in OTP.")
    }
}