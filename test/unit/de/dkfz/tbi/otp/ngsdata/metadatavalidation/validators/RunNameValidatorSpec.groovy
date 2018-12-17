package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class RunNameValidatorSpec extends Specification {

    void 'validate valid run name, succeeds'() {

        String ARBITRARY_RUN_NAME = "120111_SN509_0137_BD0CWYACXX"

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_ID}\n" +
                "${ARBITRARY_RUN_NAME}\n"
        )

        when:
        new RunNameValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate invalid run name, adds problems'() {

        String ARBITRARY_RUN_NAME = "120111_SN509_/something/0137_BD0CWYACXX"

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_ID}\n" +
                "${ARBITRARY_RUN_NAME}\n"
        )

        when:
        new RunNameValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The run name '${ARBITRARY_RUN_NAME}' is not a valid directory name.")
    }

}
