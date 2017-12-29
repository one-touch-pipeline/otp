package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class RunDateValidatorSpec extends Specification {

    void 'validate context with 3 errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_DATE}\n" +
                        "2016-01-01\n" +
                        "2016-01-32\n" +
                        "2016-0s1-22\n" +
                        "9999-01-01\n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The format of the run date '2016-01-32' is invalid, it must match yyyy-MM-dd.", "The format of at least one run date is invalid, it must match yyyy-MM-dd."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The format of the run date '2016-0s1-22' is invalid, it must match yyyy-MM-dd.", "The format of at least one run date is invalid, it must match yyyy-MM-dd."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The run date '9999-01-01' must not be from the future.", "No run date may be from the future."),
                ]

        when:
        new RunDateValidator().validate(context)


        then:
        containSame(expectedProblems, context.problems)
    }

    void 'validate context without RUN_DATE column'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new RunDateValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Mandatory column 'RUN_DATE' is missing."
    }
}