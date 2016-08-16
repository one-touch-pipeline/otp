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
