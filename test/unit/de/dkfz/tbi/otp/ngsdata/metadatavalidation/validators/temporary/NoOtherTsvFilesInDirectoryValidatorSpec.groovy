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

    void 'validate, when directory contains other .tsv file, adds error'() {

        given:
        File testDirectory = temporaryFolder.newFolder()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(testDirectory: testDirectory)
        assert context.metadataFile.parentFile.mkdir()
        CreateFileHelper.createFile(new File(context.metadataFile.parentFile, 'otherMetadata.tsv'))

        when:
        new NoOtherTsvFilesInDirectoryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message.contains('contains other *.tsv files')
    }

    void 'validate, when directory contains no other .tsv file, does nothing'() {

        given:
        File testDirectory = temporaryFolder.newFolder()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(testDirectory: testDirectory)
        assert context.metadataFile.parentFile.mkdir()
        CreateFileHelper.createFile(context.metadataFile)

        when:
        new NoOtherTsvFilesInDirectoryValidator().validate(context)

        then:
        context.problems.isEmpty()
    }
}
