package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([AntibodyTarget])
class AntibodyTargetValidatorSpec extends Specification {

    void 'validate, when no ANTIBODY_TARGET column exists in metadata file, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new AntibodyTargetValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when ANTIBODY_TARGET column is empty, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        ""
        )

        when:
        new AntibodyTargetValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when ANTIBODY_TARGET entry is not registered in database, adds errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "some_antibody_target"
        )

        when:
        new AntibodyTargetValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The antibody target 'some_antibody_target' is not registered in OTP.")
    }

    void 'validate, when ANTIBODY_TARGET entry is registered in database, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "some_antibody_target\n" +
                        "Some_Antibody_Target"
        )
        DomainFactory.createAntibodyTarget(name: "some_antibody_target")

        when:
        new AntibodyTargetValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when ANTIBODY_TARGET entry is a shortcut of a registered entry in database, adds errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "target"
        )
        DomainFactory.createAntibodyTarget(name: "some_antibody_target")

        when:
        new AntibodyTargetValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The antibody target 'target' is not registered in OTP.")
    }

    void 'validate, when ANTIBODY_TARGET entry contains special character, adds errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${ANTIBODY_TARGET}\n" +
                        "some_%_target"
        )
        DomainFactory.createAntibodyTarget(name: "some_antibody_target")

        when:
        new AntibodyTargetValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The antibody target 'some_%_target' is not registered in OTP.")
    }
}
