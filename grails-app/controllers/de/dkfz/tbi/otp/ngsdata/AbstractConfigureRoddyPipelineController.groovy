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

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.RoddyConfiguration

@PreAuthorize("hasRole('ROLE_OPERATOR')")
abstract class AbstractConfigureRoddyPipelineController extends AbstractConfigurePipelineController implements ConfigurePipelineHelper {

    static allowedMethods = [
            index: "GET",
            save: "POST",
    ]

    Map index(BaseConfigurePipelineSubmitCommand cmd) {
        Project project = projectSelectionService.selectedProject
        Map result = [:]
        result << getValues(project, cmd.seqType, pipeline)

        String pluginName = getDefaultPluginName(cmd.seqType.roddyName)
        String programVersion = getDefaultProgramVersion(cmd.seqType.roddyName)
        String baseProjectConfig = getDefaultBaseProjectConfig(cmd.seqType.roddyName)
        result << [
                pluginName              : pluginName,
                defaultPluginName       : pluginName,

                programVersion          : programVersion,
                defaultProgramVersion   : programVersion,

                baseProjectConfig       : baseProjectConfig,
                defaultBaseProjectConfig: baseProjectConfig,
        ]
        return result
    }

    def save(ConfigurePipelineSubmitCommand cmd) {
        Project project = projectSelectionService.requestedProject
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String, cmd.errors)
            flash.cmd = cmd
            redirect(action: "index", params: ["seqType.id": cmd.seqType.id])
            return
        }

        if (!validateUniqueness(cmd, project, pipeline)) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String,
                    [g.message(code: "configurePipeline.store.failure.duplicate") as String])
            flash.cmd = cmd
            redirect(action: "index", params: ["seqType.id": cmd.seqType.id])
            return
        }

        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : project,
                seqType          : cmd.seqType,
                pluginName       : cmd.pluginName,
                programVersion   : cmd.programVersion,
                baseProjectConfig: cmd.baseProjectConfig,
                configVersion    : cmd.config,
        ])
        configure(configuration)

        flash.message = new FlashMessage(flash.message = g.message(code: "configurePipeline.store.success") as String)
        redirect(controller: "analysisConfigurationOverview")
    }

    protected abstract String getDefaultPluginName(String roddyName)
    protected abstract String getDefaultProgramVersion(String roddyName)
    protected abstract String getDefaultBaseProjectConfig(String roddyName)
    protected abstract void configure(RoddyConfiguration configuration)
}
