package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.REFERENCE_GENOME
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

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