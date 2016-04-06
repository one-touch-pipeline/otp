package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.util.regex.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory.*

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
                    return new File(dir, matcher.group(1))
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
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_empty')}'."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_a_file')}' is not a file."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'empty')}' is empty."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set<Cell>,
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found1')}'."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_found1')}' could not be found by OTP."),
                new Problem((context.spreadsheet.dataRows[7].cells + context.spreadsheet.dataRows[8].cells) as Set<Cell>,
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found2')}'."),
                new Problem((context.spreadsheet.dataRows[7].cells + context.spreadsheet.dataRows[8].cells) as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_found2')}' could not be found by OTP."),
                new Problem((context.spreadsheet.dataRows[ 9].cells + context.spreadsheet.dataRows[10].cells +
                             context.spreadsheet.dataRows[11].cells + context.spreadsheet.dataRows[12].cells) as Set<Cell>,
                        Level.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found3')}'."),
                new Problem((context.spreadsheet.dataRows[ 9].cells + context.spreadsheet.dataRows[10].cells +
                             context.spreadsheet.dataRows[11].cells + context.spreadsheet.dataRows[12].cells) as Set<Cell>,
                        Level.ERROR, "'${new File(dir, 'not_found3')}' could not be found by OTP."),
        ]

        when:
        new DataFileExistenceValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

}
