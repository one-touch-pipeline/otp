package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class Md5sumFormatValidatorSpec extends Specification {

    void 'validate concerning metadata, adds expected error'() {

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

    void 'validate concerning bam metadata, allows empty cells'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${BamMetadataColumn.MD5}\n" +
                        "\n"
        )

        when:
        new Md5sumFormatValidator().validate(context)

        then:
        context.problems.empty
    }
}
