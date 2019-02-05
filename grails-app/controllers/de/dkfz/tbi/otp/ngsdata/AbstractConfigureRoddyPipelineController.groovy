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

import de.dkfz.tbi.otp.FlashMessage

abstract class AbstractConfigureRoddyPipelineController extends AbstractConfigurePipelineController implements ConfigurePipelineHelper {

    Map index(ConfigurePipelineSubmitCommand cmd) {
        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            RoddyConfiguration configuration = new RoddyConfiguration([
                    project          : cmd.project,
                    seqType          : cmd.seqType,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    configVersion    : cmd.config,
            ])
            configure(configuration)

            flash.message = new FlashMessage(flash.message = g.message(code: "configurePipeline.store.success") as String)
            redirect(controller: "projectConfig")
        }

        result << params
        result << getValues(cmd.project, cmd.seqType, getPipeline())

        String pluginName = getDefaultPluginName(cmd.seqType.roddyName)
        String pluginVersion = getDefaultPluginVersion(cmd.seqType.roddyName)
        String baseProjectConfig = getDefaultBaseProjectConfig(cmd.seqType.roddyName)
        result << [
                pipeline                : getPipeline(),

                pluginName              : pluginName,
                defaultPluginName       : pluginName,

                pluginVersion           : pluginVersion,
                defaultPluginVersion    : pluginVersion,

                baseProjectConfig       : baseProjectConfig,
                defaultBaseProjectConfig: baseProjectConfig,
        ]
        return result
    }

    abstract String getDefaultPluginName(String roddyName)
    abstract String getDefaultPluginVersion(String roddyName)
    abstract String getDefaultBaseProjectConfig(String roddyName)
    abstract void configure(RoddyConfiguration configuration)
}
