package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*
import java.util.regex.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory.*

@Mock([
        Realm,
])
class DataFileExistenceValidatorSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    void 'validate adds expected problems'() {

        given:

        File dir = temporaryFolder.root
        temporaryFolder.newFolder('not_a_file')
        temporaryFolder.newFile('empty')
        CreateFileHelper.createFile(new File(dir, 'not_empty'))

        DirectoryStructure directoryStructure = Mock(DirectoryStructure) {
            getDescription() >> 'test directory structure'
            getColumnTitles() >> ['FILENAME']
            getDataFilePath(_, _) >> { MetadataValidationContext context, ValueTuple valueTuple ->
                Matcher matcher = valueTuple.getValue('FILENAME') =~ /^(.+) .$/
                if (matcher) {
                    return Paths.get(dir.path, matcher.group(1))
                } else {
                    return null
                }
            }
        }
        MetadataValidationContext context = createContext(
                "FILENAME\n" +
                        "invalid\n" +
                        "not_empty A\n" +
                        "not_empty A\n" +
                        "not_a_file A\n" +
                        "empty A\n" +
                        "not_found1 A\n" +
                        "not_found1 A\n" +
                        "not_found2 A\n" +
                        "not_found2 B\n" +
                        "not_found3 A\n" +
                        "not_found3 A\n" +
                        "not_found3 B\n" +
                        "not_found3 B\n",
                [directoryStructure: directoryStructure]
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(),
                        Level.INFO, "Using directory structure 'test directory structure'. If this is incorrect, please select the correct one."),
                new Problem((context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[2].cells) as Set<Cell>,
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_empty')}'.", "Multiple rows reference the same file."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_a_file')}' is not a file.", "At least one file can not be access by OTP, does not exist, is empty or is not a file."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'empty')}' is empty.", "At least one file can not be access by OTP, does not exist, is empty or is not a file."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set<Cell>,
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found1')}'.", "Multiple rows reference the same file."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_found1')}' does not exist or cannot be accessed by OTP.", "At least one file can not be access by OTP, does not exist, is empty or is not a file."),
                new Problem((context.spreadsheet.dataRows[7].cells + context.spreadsheet.dataRows[8].cells) as Set<Cell>,
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found2')}'.", "Multiple rows reference the same file."),
                new Problem((context.spreadsheet.dataRows[7].cells + context.spreadsheet.dataRows[8].cells) as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_found2')}' does not exist or cannot be accessed by OTP.", "At least one file can not be access by OTP, does not exist, is empty or is not a file."),
                new Problem((context.spreadsheet.dataRows[ 9].cells + context.spreadsheet.dataRows[10].cells +
                             context.spreadsheet.dataRows[11].cells + context.spreadsheet.dataRows[12].cells) as Set<Cell>,
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found3')}'.", "Multiple rows reference the same file."),
                new Problem((context.spreadsheet.dataRows[ 9].cells + context.spreadsheet.dataRows[10].cells +
                             context.spreadsheet.dataRows[11].cells + context.spreadsheet.dataRows[12].cells) as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_found3')}' does not exist or cannot be accessed by OTP.", "At least one file can not be access by OTP, does not exist, is empty or is not a file."),
        ]
        DataFileExistenceValidator validator = new DataFileExistenceValidator()

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

}
