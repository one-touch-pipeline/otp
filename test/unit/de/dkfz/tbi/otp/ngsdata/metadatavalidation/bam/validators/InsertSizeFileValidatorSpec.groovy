package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class InsertSizeFileValidatorSpec extends Specification {

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    @Unroll
    void 'validate context with errors'() {

        given:
        File bamFile = temporaryFolder.newFile('abc')
        File insertFile = temporaryFolder.newFile('insertFile')
        File dir = temporaryFolder.newFolder('folder')
        File notReadAble = temporaryFolder.newFile('abcde')
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.absolutePath} && echo OK").trim() == 'OK'
        assert !notReadAble.canRead()

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${BAM_FILE_PATH}\t${INSERT_SIZE_FILE}\n" +
                "${bamFile.absolutePath}\ttestFile\n" +
                "\t/abc/testFile\n" +
                "${bamFile.absolutePath}\t../testFile\n" +
                "${bamFile.absolutePath}\t./testFile\n" +
                "${bamFile.absolutePath}\t${dir.name}\n" +
                "${bamFile.absolutePath}\t${notReadAble.name}\n" +
                "\tabc/testFile\n" + //ignored because of no bamFilePath
                "${bamFile.absolutePath}\t${insertFile.name}\n" // valid
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "'testFile' does not exist or cannot be accessed by OTP.", "At least one file does not exist or cannot be accessed by OTP."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The path '/abc/testFile' is not a relative path.", "At least one path is not a relative path."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The path '../testFile' is not a relative path.", "At least one path is not a relative path."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The path './testFile' is not a relative path.", "At least one path is not a relative path."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "'${dir.name}' is not a file.", "At least one file is not a file."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "'${notReadAble.name}' is not readable.", "At least one file is not readable."),
        ]

        when:
        new InsertSizeFileValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void 'validate, when column INSERT_SIZE_FILE is missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${PROJECT}\t +" +
                        "${INDIVIDUAL}\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "'${INSERT_SIZE_FILE}' has to be set for Sophia", "'${INSERT_SIZE_FILE}' has to be set for Sophia")
        ]

        when:
        new InsertSizeFileValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
