package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import spock.lang.Specification
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class FilenameValidatorSpec extends Specification {

    void 'validate context with 2 errors and 1 warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                        "test_fastq.gz\n" +
                        "test_fastq.gzz\n" +
                        "test_fast.gz\n" +
                        "öäü_fastq.gz\n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "Filename must end with '.gz'."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "Filename should contain 'fastq'."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "Filename contains invalid characters."),
        ]

        when:
        new FilenameValidator().validate(context)

        then:
        containSame(expectedProblems, context.problems)
    }

    void 'validate context without FASTQ_FILE column'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new FilenameValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Mandatory column 'FASTQ_FILE' is missing."
    }
}
