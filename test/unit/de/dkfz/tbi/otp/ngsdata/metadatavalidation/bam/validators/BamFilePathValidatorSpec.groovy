package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([FileType])
class BamFilePathValidatorSpec extends Specification {

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    @Unroll
    void 'validate context with errors'() {

        given:
        File wrongFormatFile = temporaryFolder.newFile('test.xls')
        File file = temporaryFolder.newFile('abc.bam')
        File dir = temporaryFolder.newFolder('folder.bam')
        File notReadAble = temporaryFolder.newFile('abcde.bam')
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.absolutePath} && echo OK").trim() == 'OK'
        assert !notReadAble.canRead()

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${BAM_FILE_PATH}\n" +
                        "testFile.bam\n" +
                        "abc/testFile.bam\n" +
                        "../testFile.bam\n" +
                        "./testFile.bam\n" +
                        "${wrongFormatFile}\n" +
                        "/tmp/test.bam\n" +
                        "${dir.absolutePath}\n" +
                        "${notReadAble.absolutePath}\n" +
                        "${file.absolutePath}\n" // valid

        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The path 'testFile.bam' is not an absolute path.", "At least one path is not an absolute path."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The path 'abc/testFile.bam' is not an absolute path.", "At least one path is not an absolute path."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The path '../testFile.bam' is not an absolute path.", "At least one path is not an absolute path."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The path './testFile.bam' is not an absolute path.", "At least one path is not an absolute path."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "Filename '${wrongFormatFile}' does not end with '.bam'.", "At least one filename does not end with '.bam'."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                         "'/tmp/test.bam' does not exist or cannot be accessed by OTP.", "At least one file does not exist or cannot be accessed by OTP."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                         "'${dir.absolutePath}' is not a file.", "At least one file is not a file."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                          "'${notReadAble.absolutePath}' is not readable.", "At least one file is not readable."),
        ]

        when:
        new BamFilePathValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void 'validate, when column BAM_FILE_PATH missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${PROJECT}\t +" +
                "${INDIVIDUAL}\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column '${BAM_FILE_PATH}' is missing.")
        ]

        when:
        new BamFilePathValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
