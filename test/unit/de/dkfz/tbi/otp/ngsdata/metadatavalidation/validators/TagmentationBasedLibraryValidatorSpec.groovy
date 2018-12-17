package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame

class TagmentationBasedLibraryValidatorSpec extends Specification {

    void 'validate adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\n" +
                        "1\n" +
                        "0\n" +
                        "True\n" +
                        "TRUE\n" +
                        "true\n" +
                        "false\n" +
                        "FALSE\n" +
                        "test\n" +
                        "\n")
        Collection<Problem> expectedProblems = [

                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The tagmentation based library column value should be '1', 'true', 'false' or an empty string instead of '0'.", "The tagmentation based library column value should be '1', 'true', 'false' or an empty string."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The tagmentation based library column value should be '1', 'true', 'false' or an empty string instead of 'test'.", "The tagmentation based library column value should be '1', 'true', 'false' or an empty string."),
        ]

        when:
        new TagmentationBasedLibraryValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
