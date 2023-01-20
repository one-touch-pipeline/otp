/*
 * Copyright 2011-2021 The OTP authors
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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.Individual

import java.nio.file.Path

@CompileDynamic
@Transactional
class RoddyCommandService {

    ProcessingOptionService processingOptionService

    String createRoddyCommand(Individual individual, Path confDir, List<String> additionalParams = []) {
        assert individual
        assert confDir

        return [
                activateModulesForRoddyCommand(),
                roddyCommand(individual, confDir, additionalParams),
        ].join('\n')
    }

    private String activateModulesForRoddyCommand() {
        String loadModule = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
        String activateJava = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA)
        String activateGroovy = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY)

        return [
                loadModule,
                activateJava,
                activateGroovy,
        ].findAll().join('\n')
    }

    private String roddyCommand(Individual individual, Path confDir, List<String> additionalParams) {
        String roddyPath = processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_PATH)
        String applicationIniPath = processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_APPLICATION_INI)
        String featureTogglesConfigPath = processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH)

        return ([
                "${roddyPath}/roddy.sh",
                "rerun",
                "${RoddyConfigService.CONFIGURATION_NAME}@${RoddyConfigService.ANALYSIS_ID}",
                "${individual.pid}",
                "--useconfig=${applicationIniPath}",
                "--usefeaturetoggleconfig=${featureTogglesConfigPath}",
                "--configurationDirectories=${confDir}",
        ] + additionalParams).join(" ")
    }
}
