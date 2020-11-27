/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.project.RoddyConfiguration

@Secured("hasRole('ROLE_OPERATOR')")
class ConfigureSnvPipelineController extends AbstractConfigureRoddyPipelineController {

    @Override
    Pipeline getPipeline() {
        Pipeline.Name.RODDY_SNV.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultPluginName(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultProgramVersion(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultBaseProjectConfig(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG, roddyName)
    }

    @Override
    void configure(RoddyConfiguration configuration) {
        projectService.configureSnvPipelineProject(configuration)
    }
}
