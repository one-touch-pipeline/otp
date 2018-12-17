package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock([
        FileType,
])
class FilenameValidatorSpec extends Specification {

    void 'validate context with errors and warnings'() {

        given:
        DomainFactory.createFileType(signature: '_fastq')
        DomainFactory.createFileType(signature: '.fastq')
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                        "test_fastq.gz\n" +
                        "test.fastq.gz\n" +
                        "test_fastq.gzz\n" +
                        "test_fast.gz\n" +
                        "öäü_fastq.gz\n" +
                        "/tmp/test_fastq.gz\n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "Filename 'test_fastq.gzz' does not end with '.gz'.", "At least one filename does not end with '.gz'."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "Filename 'test_fast.gz' contains neither '_fastq' nor '.fastq'.", "At least one filename contains neither '_fastq' nor '.fastq'."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "Filename 'öäü_fastq.gz' contains invalid characters.", "At least one filename contains invalid characters."),
        ]

        when:
        new FilenameValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
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
