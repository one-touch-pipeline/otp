package de.dkfz.tbi.otp.utils

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Mock([
        ProcessingOption,
])
class ExecuteRoddyCommandServiceWithoutAutowireSpec extends Specification {

    void "check activateModulesForRoddyCommand with modules, should return activation commands"() {
        given:
        String loadModule = "LOAD MODULE"
        String activateJava = "ACTIVATE JAVA"
        String activateGroovy = "ACTIVATE_Groovy"
        ExecuteRoddyCommandService service = new ExecuteRoddyCommandService([
                processingOptionService: new ProcessingOptionService(),
        ])

        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, loadModule)
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, activateJava)
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, activateGroovy)

        String expected = [
                loadModule,
                activateJava,
                activateGroovy,
        ].join('\n')

        when:
        String result = service.activateModulesForRoddyCommand()

        then:
        expected == result
    }

    void "check activateModulesForRoddyCommand without modules, should return empty string"() {
        given:
        ExecuteRoddyCommandService service = new ExecuteRoddyCommandService([
                processingOptionService: new ProcessingOptionService(),
        ])

        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, '')
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, '')
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, '')

        when:
        String result = service.activateModulesForRoddyCommand()

        then:
        result.empty
    }

}
