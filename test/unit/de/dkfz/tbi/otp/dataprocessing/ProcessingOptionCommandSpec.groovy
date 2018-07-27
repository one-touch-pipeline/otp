package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        ProcessingOption,
])
class ProcessingOptionCommandSpec extends Specification {

    @Unroll
    void "validate #optionName with #value, should be valid"() {
        when:
        ProcessingOptionCommand processingOptionCommand = new ProcessingOptionCommand(
                optionName: optionName,
                type: null,
                value: value,
                project: HelperUtils.uniqueString,
                submit: HelperUtils.uniqueString,
                projectService: Mock(ProjectService) {
                    1 * getProjectByName(_) >> { new Project() }
                }
        )
        then:
        processingOptionCommand.validate()

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'true'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'false'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | '5'
    }

    @Unroll
    void "validate #optionName with #value, should be invalid"() {
        when:
        ProcessingOptionCommand processingOptionCommand = new ProcessingOptionCommand(
                optionName: optionName,
                type: null,
                value: value,
                project: HelperUtils.uniqueString,
                submit: HelperUtils.uniqueString,
                projectService: Mock(ProjectService) {
                    1 * getProjectByName(_) >> { new Project() }
                }
        )
        then:
        TestCase.assertValidateError(processingOptionCommand, 'value', 'validator.invalid', value)

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 't'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'f'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | 'text'
    }


}
