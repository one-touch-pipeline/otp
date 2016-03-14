package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import grails.test.mixin.Mock
import spock.lang.Specification

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock([Project])
class ProjectValidatorSpec extends Specification {

    void 'validate, when column does not exist, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new ProjectValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when column is empty, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n"
        )

        when:
        new ProjectValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when column exist and project is registered in OTP, succeeds'() {

        given:
        String PROJECT_NAME = "projectName"

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${PROJECT_NAME}\n" +
                        "${PROJECT_NAME}_1"
        )

        DomainFactory.createProject([name: PROJECT_NAME])
        DomainFactory.createProject([nameInMetadataFiles: "${PROJECT_NAME}_1"])

        when:
        new ProjectValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when column exist but project is not registered in OTP, adds problems'() {

        given:
        String PROJECT_NAME = "projectName"

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${PROJECT_NAME}\n"
        )

        when:
        new ProjectValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The project '${PROJECT_NAME}' is not registered in OTP.")
    }

}
