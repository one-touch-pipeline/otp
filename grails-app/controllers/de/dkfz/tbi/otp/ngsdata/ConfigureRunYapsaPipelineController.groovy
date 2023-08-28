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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.project.Project

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ConfigureRunYapsaPipelineController extends AbstractConfigureNonRoddyPipelineController {

    static allowedMethods = [
            index: "GET",
            update: "POST",
    ]

    def index(BaseConfigurePipelineSubmitCommand cmd) {
        return getModelValues(cmd.seqType)
    }

    def update(ConfigureRunYapsaSubmitCommand cmd) {
        updatePipeline(
                projectService.createOrUpdateRunYapsaConfig(projectSelectionService.requestedProject, cmd.seqType, cmd.programVersion),
                cmd.seqType,
                cmd.overviewController,
        )
    }

    @Override
    protected Pipeline getPipeline() {
        Pipeline.Name.RUN_YAPSA.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') // for an unknown reason the groovy compiler doesnt work with @Override in this case
    protected ConfigPerProjectAndSeqType getLatestConfig(Project project, SeqType seqType) {
        return projectService.getLatestRunYapsaConfig(project, seqType)
    }

    @Override
    protected String getDefaultVersion() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_DEFAULT_VERSION)
    }

    @Override
    protected List<String> getAvailableVersions() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_AVAILABLE_VERSIONS)
    }
}
