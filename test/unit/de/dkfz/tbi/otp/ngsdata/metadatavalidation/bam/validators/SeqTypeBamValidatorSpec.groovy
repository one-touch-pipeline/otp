package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([SeqType])
class SeqTypeBamValidatorSpec extends Specification {

    void 'validate, when column SEQUENCING_TYPE missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "SomeColumn\n"+
                        "SomeValue"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column '${SEQUENCING_TYPE}' is missing.")
        ]

        when:
        new SeqTypeBamValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when column exist and seqType is registered in OTP, succeeds'() {

        given:
        String SEQ_TYPE_NAME = "seqTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                        "${SEQ_TYPE_NAME}\n"
        )

        DomainFactory.createSeqType([name: SEQ_TYPE_NAME])

        when:
        new SeqTypeBamValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist but seqType is not registered in OTP, adds problems'() {

        given:
        String SEQ_TYPE_NAME = "seqTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\n" +
                        "${SEQ_TYPE_NAME}\n"
        )

        when:
        new SeqTypeBamValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The sequencing type '${SEQ_TYPE_NAME}' is not registered in OTP.")
    }
}