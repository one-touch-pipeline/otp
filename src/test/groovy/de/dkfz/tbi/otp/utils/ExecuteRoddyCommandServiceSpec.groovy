/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.utils

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class ExecuteRoddyCommandServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
        ]
    }

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
