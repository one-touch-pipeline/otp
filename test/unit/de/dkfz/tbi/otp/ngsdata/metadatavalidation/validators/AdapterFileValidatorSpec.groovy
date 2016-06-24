package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*


@Mock([
        AdapterFile,
])
class AdapterFileValidatorSpec extends Specification {

    static final String adapterName = "adapter"

    static final String VALID_METADATA =
        "${ADAPTER_FILE.name()}\n" +
        "${adapterName}\n"

    void setup() {
        DomainFactory.createAdapterFile(fileName: adapterName).save(flush: true, failOnError: true)
    }


    void 'validate, when all data is valid'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        AdapterFileValidator validator = new AdapterFileValidator()
        validator.adapterFileService = new AdapterFileService()
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column is missing, is valid'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("other_column")

        when:
        AdapterFileValidator validator = new AdapterFileValidator()
        validator.adapterFileService = new AdapterFileService()
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when adapter file not registered, is not valid'() {

        given:
        String notRegistered = "notRegistered"
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "${notRegistered}\n"
        )

        when:
        AdapterFileValidator validator = new AdapterFileValidator()
        validator.adapterFileService = new AdapterFileService()
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A3'])
        problem.message.contains("Adapter file name '${notRegistered}' is not registered in OTP.")
    }
}
