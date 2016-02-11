package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.temporary

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.Specification

class NoOtherTsvFilesInDirectoryValidatorSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    void 'validate, when directory contains other importable files, adds warning'() {

        given:
        File testDirectory = temporaryFolder.newFolder()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(testDirectory: testDirectory)
        CreateFileHelper.createFile(context.metadataFile)
        CreateFileHelper.createFile(new File(context.metadataFile.parentFile, 'metadata_align.tsv'))
        CreateFileHelper.createFile(new File(context.metadataFile.parentFile, 'otherMetadata_fastq.tsv'))

        when:
        new NoOtherTsvFilesInDirectoryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message.startsWith("Directory '${context.metadataFile.parentFile}' contains multiple files which would be imported:\n'")
        problem.message.contains("'${context.metadataFile.name}'")
        problem.message.contains("'metadata_align.tsv'")
        problem.message.contains("'otherMetadata_fastq.tsv'")
    }

    void "validate, when file name does not contain 'fastq', adds error"() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                metadataFile: temporaryFolder.newFile('metadata.tsv'))
        CreateFileHelper.createFile(context.metadataFile)
        CreateFileHelper.createFile(new File(context.metadataFile.parentFile, 'otherMetadata_fastq.tsv'))

        when:
        new NoOtherTsvFilesInDirectoryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "The file name of '${context.metadataFile}' does not contain 'fastq'."
    }

    void 'validate, when directory contains other non-importable .tsv file, does nothing'() {

        given:
        File testDirectory = temporaryFolder.newFolder()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(testDirectory: testDirectory)
        CreateFileHelper.createFile(context.metadataFile)
        CreateFileHelper.createFile(new File(context.metadataFile.parentFile, 'metadata.tsv'))

        when:
        new NoOtherTsvFilesInDirectoryValidator().validate(context)

        then:
        context.problems.isEmpty()
    }
    void 'validate, when directory contains no other .tsv file, does nothing'() {

        given:
        File testDirectory = temporaryFolder.newFolder()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(testDirectory: testDirectory)
        CreateFileHelper.createFile(context.metadataFile)

        when:
        new NoOtherTsvFilesInDirectoryValidator().validate(context)

        then:
        context.problems.isEmpty()
    }
}
