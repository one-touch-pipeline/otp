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

import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType

abstract class AbstractConfigureNonRoddyPipelineController extends AbstractConfigurePipelineController {

    ProjectSelectionService projectSelectionService

    Map index(BaseConfigurePipelineSubmitCommand cmd) {
        Project project = projectSelectionService.selectedProject
        ConfigPerProjectAndSeqType config = getLatestConfig(project, cmd.seqType)
        String currentVersion = config?.programVersion

        String defaultVersion = getDefaultVersion()
        List<String> availableVersions = getAvailableVersions()

        return [
                project: project,
                seqType: cmd.seqType,
                pipeline: getPipeline(),

                defaultVersion: defaultVersion,
                currentVersion: currentVersion,
                availableVersions: availableVersions,
        ] + getAdditionalProperties(project, cmd.seqType)
    }

    void updatePipeline(Errors errors, SeqType seqType) {
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String, errors)
            redirect action: "index", params: ['seqType.id': seqType.id]
        } else {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.success") as String)
            redirect controller: "projectConfig"
        }
    }

    @SuppressWarnings("UnusedMethodParameter")
    Map getAdditionalProperties(Project project, SeqType seqType) {
        return [:]
    }

    abstract ConfigPerProjectAndSeqType getLatestConfig(Project project, SeqType seqType)

    abstract String getDefaultVersion()

    abstract List<String> getAvailableVersions()
}
