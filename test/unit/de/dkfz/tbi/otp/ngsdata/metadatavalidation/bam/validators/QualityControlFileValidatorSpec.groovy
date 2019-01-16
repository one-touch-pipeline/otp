package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*

class QualityControlFileValidatorSpec extends Specification {

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    @Unroll
    void 'validate context with errors'() {

        given:
        File bamFile = temporaryFolder.newFile('abc')
        File dir = temporaryFolder.newFolder('folder')
        File notReadAble = temporaryFolder.newFile('abcde')
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.absolutePath} && echo OK").trim() == 'OK'
        assert !notReadAble.canRead()

        File qualityControl = temporaryFolder.newFile("qualityControl.json")
        qualityControl.toPath().bytes = ("""\
        { "all":{"insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157,  "properlyPaired": 2050531101 }}
        """).getBytes(BamMetadataValidationContext.CHARSET)

        File qualityControlInvalid = temporaryFolder.newFile("qualityControlInvalid.json")
        qualityControlInvalid.toPath().bytes = ("""\
        { "all":{"insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157}}
        """).getBytes(BamMetadataValidationContext.CHARSET)

        File qualityControlInvalidJson = temporaryFolder.newFile("qualityControlInvalidJson")
        qualityControlInvalidJson.toPath().bytes = ("""\
        "insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157
        """).getBytes(BamMetadataValidationContext.CHARSET)


        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${BAM_FILE_PATH}\t${QUALITY_CONTROL_FILE}\n" +
                        "${bamFile.absolutePath}\ttestFile\n" +
                        "\t/abc/testFile\n" +
                        "${bamFile.absolutePath}\t../testFile\n" +
                        "${bamFile.absolutePath}\t./testFile\n" +
                        "${bamFile.absolutePath}\t${dir.name}\n" +
                        "${bamFile.absolutePath}\t${notReadAble.name}\n" +
                        "\tabc/testFile\n" + //ignored because of no bamFilePath
                        "${bamFile.absolutePath}\t${qualityControlInvalid.name}\n" + // invalid
                        "${bamFile.absolutePath}\t${qualityControlInvalidJson.name}\n" + // invalid
                        "${bamFile.absolutePath}\t${qualityControl.name}\n" // valid
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "'testFile' does not exist or cannot be accessed by OTP.",
                        "At least one file does not exist or cannot be accessed by OTP."),
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
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "'${qualityControlInvalid.name}' has not all needed values or has no valid JSON structure.",
                        "At least one value is missing or has no valid JSON structure."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "'${qualityControlInvalidJson.name}' has not all needed values or has no valid JSON structure.",
                        "At least one value is missing or has no valid JSON structure.")
        ]

        when:
        new QualityControlFileValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void 'validate, when mandatory column BAM_FILE_PATH is missing, then expected error'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${PROJECT}\t${QUALITY_CONTROL_FILE}\n\nproject\t/tmp/file\n"
        )

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column 'BAM_FILE_PATH' is missing.",
                        "Mandatory column 'BAM_FILE_PATH' is missing.")
        ]

        when:
        new QualityControlFileValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when optional column QUALITY_CONTROL_FILE is missing, then expected warning'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${PROJECT}\t${BAM_FILE_PATH}\nproject\t/tmp/file\n"
        )

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "'QUALITY_CONTROL_FILE' has to be set for Sophia",
                        "'QUALITY_CONTROL_FILE' has to be set for Sophia"),
        ]

        when:
        new QualityControlFileValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}
