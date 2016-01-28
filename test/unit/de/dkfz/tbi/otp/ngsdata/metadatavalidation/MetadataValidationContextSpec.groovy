package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.junit.ClassRule
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

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
        TestCase.uniqueNonExistentPath                                              || 'could not be found by OTP'
        temporaryFolder.newFolder()                                                 || 'is not a file'
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
        context.metadataFileMd5sum.equalsIgnoreCase(md5sum)
        context.spreadsheet.header.cells[0].text ==~ firstCellRegex

        where:
        bytes || md5sum | firstCellRegex | problemMessage
        'M\u00e4use'.getBytes('ISO-8859-1') || 'a8a625101e3dae99e646f9c392ede3d4' | '^M.{0,2}use$' | 'not properly encoded with UTF-8'
        'a"b'.getBytes(MetadataValidationContext.CHARSET) || 'c8f88ec84680a7ec056720570290dd34' | '^a.{0,2}b$' | 'contains one or more quotation marks'
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
        context.metadataFileMd5sum.equalsIgnoreCase('51cfcea2dc88d9baff201d447d2316df')
        context.spreadsheet == null
    }

    void 'createFromFile, when file can be parsed successfully, does not add problems'() {
        given:
        File file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv")
        file.bytes = 'M\u00e4use'.getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = MetadataValidationContext.createFromFile(file, directoryStructure)

        then:
        context.problems.isEmpty()
        context.directoryStructure == directoryStructure
        context.metadataFile == file
        context.metadataFileMd5sum.equalsIgnoreCase('9e904c83916d4e86082bd6ebe0d5339e')
        context.spreadsheet.header.cells[0].text == 'M\u00e4use'
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
