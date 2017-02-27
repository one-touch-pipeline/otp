package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class TagmentationBasedLibraryValidatorSpec extends Specification {

    void 'validate adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\n" +
                        "1\n" +
                        "0\n" +
                        "\n")

        when:
        new TagmentationBasedLibraryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A3'])
        problem.message.contains("The tagmentation based library column value should be '1' or an empty string instead of '0'")
    }
}
