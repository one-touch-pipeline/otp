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

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.project.RoddyConfiguration

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ConfigureSophiaPipelineController extends AbstractConfigureRoddyPipelineController {

    @Override
    protected Pipeline getPipeline() {
        Pipeline.Name.RODDY_SOPHIA.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') // for an unknown reason the groovy compiler doesnt work with @Override in this case
    protected String getDefaultPluginName(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') // for an unknown reason the groovy compiler doesnt work with @Override in this case
    protected String getDefaultProgramVersion(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') // for an unknown reason the groovy compiler doesnt work with @Override in this case
    protected String getDefaultBaseProjectConfig(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG, roddyName)
    }

    @Override
    protected void configure(RoddyConfiguration configuration) {
        projectService.configureSophiaPipelineProject(configuration)
    }
}
