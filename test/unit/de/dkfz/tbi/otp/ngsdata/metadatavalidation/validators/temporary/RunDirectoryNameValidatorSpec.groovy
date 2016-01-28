package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.temporary

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

class RunDirectoryNameValidatorSpec extends Specification {

    static final String VALID_METADATA =
            "${MetaDataColumn.RUN_ID}\n" +
            "run1\n" +
            "run1\n"

    void 'validate, when metadata file contains more than one run, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                "run2\n" +
                "run2\n"
        )

        when:
        new RunDirectoryNameValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A3', 'A4', 'A5'])
        problem.message.contains('The metadata file contains information about more than one run')
    }

    void 'validate, when directory containing the metadata file does not have allowed name, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(VALID_METADATA)

        when:
        new RunDirectoryNameValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A3'])
        problem.message.contains('The directory containing the metadata file must be named')
    }

    @Unroll
    void 'validate, when directory containing the directory containing the metadata file also contains directory with other allowed name, adds error'() {

        given:
        File testDirectory = TestCase.createEmptyTestDirectory()
        File directory1 = new File(testDirectory, directoryName1)
        assert directory1.mkdir()
        File directory2 = new File(testDirectory, directoryName2)
        assert directory2.mkdir()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA,
                [metadataFile: new File(directory1, 'metadata.tsv')]
        )

        when:
        new RunDirectoryNameValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A3'])
        problem.message =~ /Cannot import .+ when .+ exists in/

        cleanup:
        testDirectory.deleteDir()

        where:
        directoryName1 | directoryName2
        'run1'         | 'runrun1'
        'runrun1'      | 'run1'
    }

    @Unroll
    void 'validate, when directory containing the metadata file has allowed name, does not add problem'() {

        given:
        File testDirectory = TestCase.createEmptyTestDirectory()
        File directory = new File(testDirectory, directoryName)
        assert directory.mkdir()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA,
                [metadataFile: new File(directory, 'metadata.tsv')]
        )

        when:
        new RunDirectoryNameValidator().validate(context)

        then:
        context.problems.isEmpty()

        where:
        directoryName | _
        'run1' | _
        'runrun1' | _
    }
}
