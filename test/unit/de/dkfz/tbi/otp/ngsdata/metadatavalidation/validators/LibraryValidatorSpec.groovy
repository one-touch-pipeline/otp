package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CUSTOMER_LIBRARY
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class LibraryValidatorSpec extends Specification {

    void 'validate, add expected custom libraries, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        "lib1\n" +
                        "lib23\n" +
                        "lib50\n" +
                        "libNA\n"
        )

        when:
        new LibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when no CUSTOMER_LIBRARY column exists in metadata file, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new LibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when CUSTOMER_LIBRARY column is empty, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        ""
        )

        when:
        new LibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when CUSTOMER_LIBRARY entry is not a valid path component, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        "CUSTOMER_LIBRARY!"
        )

        when:
        new LibraryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("Library 'CUSTOMER_LIBRARY!' contains invalid characters.")
    }

    void 'validate, when CUSTOMER_LIBRARY entry does not match regular expression, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        "CUSTOMER_LIBRARY"
        )

        when:
        new LibraryValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("Library 'CUSTOMER_LIBRARY' does not match regular expression '^(?:lib(?:[1-9]\\d*|NA)|)\$'.")
    }
}
