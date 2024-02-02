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
package de.dkfz.tbi.otp.ngsdata

import grails.validation.Validateable
import groovy.transform.ToString
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.StringUtils

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ConfigurePipelineController {

    ProjectSelectionService projectSelectionService
    ProjectService projectService
    SeqTypeService seqTypeService

    static allowedMethods = [
            invalidateConfig           : "POST",
    ]

    def invalidateConfig(InvalidateConfigurationCommand cmd) {
        if (cmd.seqType in seqTypeService.seqTypesNewWorkflowSystem && cmd.pipeline?.type == Pipeline.Type.ALIGNMENT) {
            log.debug("invalidateConfig is not available for new workflow system")
            return response.sendError(404)
        }

        boolean hasErrors = cmd.hasErrors()

        if (hasErrors) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.invalidate.failure") as String, errors)
            redirect(action: cmd.originAction, params: ["seqType.id": cmd.seqType.id])
        } else {
            projectService.invalidateProjectConfig(projectSelectionService.requestedProject, cmd.seqType, cmd.pipeline)
            flash.message = new FlashMessage(g.message(code: "configurePipeline.invalidate.success") as String)
            redirect(controller: cmd.overviewController)
        }
    }
}

@ToString(includeNames = true, includeSuper = true)
class ConfigureRunYapsaSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String programVersion
    String overviewController
}

@ToString(includeNames = true, includeSuper = true)
class ConfigureCellRangerSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    ReferenceGenomeIndex referenceGenomeIndex
    String programVersion
    String overviewController
}

@ToString(includeNames = true, includeSuper = true)
class ConfigurePipelineSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String pluginName
    String programVersion
    String baseProjectConfig
    String config
    String submit

    static constraints = {
        pluginName nullable: true, blank: false, shared: 'pathComponent'
        programVersion nullable: true, blank: false, shared: 'pathComponent'
        baseProjectConfig nullable: false, blank: false, shared: 'pathComponent'
        config nullable: true, blank: false, validator: { val, obj ->
            if (val && !(val ==~ /^v\d+_\d+$/)) {
                return "mismatch"
            }
        }
    }

    void setConfig(String config) {
        this.config = StringUtils.trimAndShortenWhitespace(config)
    }
}

@ToString(includeNames = true, includeSuper = true)
class InvalidateConfigurationCommand extends BaseConfigurePipelineSubmitCommand {
    Pipeline pipeline
    String originAction
    String overviewController

    static constraints = {
        pipeline(nullable: false)
        originAction(nullable: false)
        overviewController(nullable: false)
    }
}

@ToString(includeNames = true)
class BaseConfigurePipelineSubmitCommand implements Validateable {
    SeqType seqType

    static constraints = {
        seqType(nullable: false)
    }
}
