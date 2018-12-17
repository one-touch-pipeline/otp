package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.AntibodyTarget
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY_TARGET
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

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
