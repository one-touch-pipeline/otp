package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import grails.test.mixin.*
import spock.lang.*

@Mock([
        ProcessingOption,
])
class ProcessingOptionSpec extends Specification {

    @Unroll
    void "validate #optionName with #value, should be valid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: optionName,
                type: null,
                value: value,

        )
        then:
        processingOption.validate()

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'true'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'false'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | '5'
    }

    @Unroll
    void "validate #optionName with #value, should be invalid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: optionName,
                type: null,
                value: value,

        )
        then:
        TestCase.assertValidateError(processingOption, 'value', 'validator.invalid', value)

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 't'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'f'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | 'text'
    }


    @Unroll
    void "validate #optionName with invalid value #value and obsolete date, should be valid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: optionName,
                type: null,
                value: value,
                dateObsoleted: new Date(),
        )
        then:
        processingOption.validate()

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 't'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'f'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | 'text'
    }

    void "validate pipeline min coverage with invalid type #type and obsolete date, should be valid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE,
                type: 'test',
                value: '20.0',
                dateObsoleted: new Date(),
        )
        then:
        processingOption.validate()
    }
}
