package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([SampleType])
class SampleTypeValidatorSpec extends Specification {

    void 'validate, when column SAMPLE_TYPE missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "SomeColumn\n"+
                        "SomeValue"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column '${SAMPLE_TYPE}' is missing.")
        ]

        when:
        new SampleTypeValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when column exist and sampleType is registered in OTP, succeeds'() {

        given:
        String SAMPLE_TYPE_NAME = "sampleTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SAMPLE_TYPE}\n" +
                        "${SAMPLE_TYPE_NAME}\n"
        )

        DomainFactory.createSampleType([name: SAMPLE_TYPE_NAME])

        when:
        new SampleTypeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist but sampleType is not registered in OTP, adds problems'() {

        given:
        String SAMPLE_TYPE_NAME = "sampleTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SAMPLE_TYPE}\n" +
                        "${SAMPLE_TYPE_NAME}\n"
        )

        when:
        new SampleTypeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The sample type '${SAMPLE_TYPE_NAME}' is not registered in OTP.")
    }
}