package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.Specification

class Md5sumFormatValidatorSpec extends Specification {

    void 'validate adds expected error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.MD5}\n" +
                "xxxxyyyyxxxxyyyyxxxxyyyyxxxxyyyy\n" +
                "aaaabbbbaaaabbbbaaaabbbbaaaabbbb\n" +
                "xxxxyyyyxxxxyyyyxxxxyyyyxxxxyyyy\n")

        when:
        new Md5sumFormatValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'A4'])
        problem.message.contains('Not a well-formatted MD5 sum')
    }
}
