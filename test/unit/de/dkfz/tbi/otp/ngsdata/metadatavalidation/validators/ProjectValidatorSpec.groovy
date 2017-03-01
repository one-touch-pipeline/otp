package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([Project, ProjectCategory,])
class ProjectValidatorSpec extends Specification {

    void 'validate concerning metadata, when column does not exist, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new ProjectValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate concerning bam metadata, when column does not exist, adds error'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext()
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column '${BamMetadataColumn.PROJECT}' is missing.")
        ]

        when:
        new ProjectValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
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


    void 'validate concerning metadata, when column exist but project is not registered in OTP, adds problems'() {

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

    void 'validate concerning bam metadata, when column exist but project is not registered in OTP, adds problems'() {

        given:
        String PROJECT_NAME = "projectName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${MetaDataColumn.PROJECT}\n" +
                        "${PROJECT_NAME}\n"
        )

        when:
        new ProjectValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The project '${PROJECT_NAME}' is not registered in OTP.")
    }

}
