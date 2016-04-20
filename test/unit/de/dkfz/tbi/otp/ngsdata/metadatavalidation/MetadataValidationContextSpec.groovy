package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class MetadataValidationContextSpec extends Specification {

    DirectoryStructure directoryStructure = [:] as DirectoryStructure

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    @Unroll
    void 'createFromFile, when file cannot be opened, adds an error'() {
        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.isEmpty()
        problem.level == Level.ERROR
        problem.message.contains(problemMessage)
        context.directoryStructure == directoryStructure
        context.metadataFile == file
        context.metadataFileMd5sum == null
        context.spreadsheet == null

        where:
        file                                                                        || problemMessage
        new File('metadata.tsv')                                                    || 'not a valid absolute path'
        new File(TestCase.uniqueNonExistentPath, 'metadata.tsv')                    || 'does not exist'
        temporaryFolder.newFolder('folder.tsv')                                     || 'is not a file'
        temporaryFolder.newFile("${HelperUtils.uniqueString}.xls")                  || "does not end with '.tsv'."
        makeNotReadable(temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")) || 'is not readable'
        temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")                  || 'is empty'
        createTooLargeMetadataFile()                                                || 'is larger than'
    }

    @Unroll
    void 'createFromFile, when there is a parsing problem, adds a warning'() {
        given:
        File file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")
        file.bytes = bytes

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.isEmpty()
        problem.level == Level.WARNING
        problem.message.contains(problemMessage)
        context.directoryStructure == directoryStructure
        context.metadataFile == file
        context.metadataFileMd5sum == md5sum
        context.spreadsheet.dataRows[0].cells[0].text ==~ firstCellRegex

        where:
        bytes || md5sum | firstCellRegex | problemMessage
        'x\nM\u00e4use'.getBytes('ISO-8859-1') || '577c31bc9f9d49ef016288cf94605c2a' | '^M.{0,2}use$' | 'not properly encoded with UTF-8'
        'x\na"b'.getBytes(MetadataValidationContext.CHARSET) || '01a73fb20c4582eb9668dc39431c4748' | '^a.{0,2}b$' | 'contains one or more quotation marks'
    }

    void 'createFromFile, when there are multiple columns with the same title, adds an error'() {
        given:
        File file = CreateFileHelper.createFile(temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv"), 'a\tb\ta')

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.isEmpty()
        problem.level == Level.ERROR
        problem.message.contains("Duplicate column 'a'")
        context.directoryStructure == directoryStructure
        context.metadataFile == file
        context.metadataFileMd5sum == '51cfcea2dc88d9baff201d447d2316df'
        context.spreadsheet == null
    }

    void 'createFromFile, when file can be parsed successfully, does not add problems'() {
        given:
        File file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")
        file.bytes = 'x\nM\u00e4use'.getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        context.problems.isEmpty()
        context.directoryStructure == directoryStructure
        context.metadataFile == file
        context.metadataFileMd5sum == '2628f03624261e75bba6960ff9d15291'
        context.spreadsheet.dataRows[0].cells[0].text == 'M\u00e4use'
    }

    void 'createFromFile removes tabs and newlines at end of file'() {
        given:
        File file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")
        file.bytes = 'a\nb\t\r\n\t\t\r\n'.getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        context.spreadsheet.dataRows.size() == 1
        context.spreadsheet.dataRows[0].cells.size() == 1
    }

    void 'createFromFile, when file contains only one line, adds error'() {
        given:
        File file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")
        file.bytes = 'a\n\n'.getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.isEmpty()
        problem.level == Level.ERROR
        problem.message.contains('contains less than two lines')
        context.spreadsheet == null
    }

    void 'pathForMessage, when path does not point to a symlink, returns the path'() {
        given:
        File targetPath = TestCase.uniqueNonExistentPath
        String expected = "'${targetPath}'"

        when:
        String actual = MetadataValidationContext.pathForMessage(targetPath)

        then:
        actual == expected
    }

    void 'pathForMessage, when path points to a symlink, returns path of symlink and path of target'() {
        given:
        File linkPath = new File(temporaryFolder.newFolder(), 'i_am_a_symlink')
        File targetPath = TestCase.uniqueNonExistentPath
        Files.createSymbolicLink(linkPath.toPath(), targetPath.toPath())
        String expected = "'${targetPath}' (linked from '${linkPath}')"

        when:
        String actual = MetadataValidationContext.pathForMessage(linkPath)

        then:
        actual == expected
    }

    void 'pathForMessage, when path points to a symlink to a symlink, returns path of symlink and path of final target'() {
        given:
        File folder = temporaryFolder.newFolder()
        File sourceSymlink = new File(folder, 'source_symlink')
        File middleSymlink = new File(folder, 'middle_symlink')
        File targetPath = TestCase.uniqueNonExistentPath
        Files.createSymbolicLink(sourceSymlink.toPath(), middleSymlink.toPath())
        Files.createSymbolicLink(middleSymlink.toPath(), targetPath.toPath())
        String expected = "'${targetPath}' (linked from '${sourceSymlink}')"

        when:
        String actual = MetadataValidationContext.pathForMessage(sourceSymlink)

        then:
        actual == expected
    }

    private static File makeNotReadable(File file) {
        file.setReadable(false)
        return file
    }

    private File createTooLargeMetadataFile() {
        return CreateFileHelper.createFile(temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv"),
                'x' * (MetadataValidationContext.MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L + 1L))
    }
}
