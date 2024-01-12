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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.Individual

import java.nio.file.Path
import java.nio.file.Paths

class RoddyCommandServiceSpec extends Specification implements ServiceUnitTest<RoddyCommandService>, DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                ProcessingOption,
        ]
    }

    void setup() {
        service.processingOptionService = new ProcessingOptionService()
    }

    void "test createRoddyCommand"() {
        given:
        String roddyPath = "/roddy"
        String applicationIni = "/etc/appl.ini"
        String featureToggles = "/data/feturetoggle.ini"
        findOrCreateProcessingOption(ProcessingOption.OptionName.RODDY_PATH, roddyPath)
        findOrCreateProcessingOption(ProcessingOption.OptionName.RODDY_APPLICATION_INI, applicationIni)
        findOrCreateProcessingOption(ProcessingOption.OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH, featureToggles)

        Individual individual = createIndividual()
        Path confDir = Paths.get("/conf/dir/")
        List<String> additionalParameters = [
                "--add=itional",
                "--para=meters",
        ]

        String expected = "\n${roddyPath}/roddy.sh rerun config@analysis ${individual.pid} --useconfig=${applicationIni} " +
                "--usefeaturetoggleconfig=${featureToggles} --configurationDirectories=${confDir} --add=itional --para=meters"

        expect:
        expected == service.createRoddyCommand(individual, confDir, additionalParameters)
    }

    void "check activateModulesForRoddyCommand with modules, should return activation commands"() {
        given:
        String loadModule = "LOAD MODULE"
        String activateJava = "ACTIVATE JAVA"
        String activateGroovy = "ACTIVATE_Groovy"
        findOrCreateProcessingOption(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, loadModule)
        findOrCreateProcessingOption(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, activateJava)
        findOrCreateProcessingOption(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, activateGroovy)

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
        findOrCreateProcessingOption(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, '')
        findOrCreateProcessingOption(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, '')
        findOrCreateProcessingOption(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, '')

        when:
        String result = service.activateModulesForRoddyCommand()

        then:
        result.empty
    }
}
