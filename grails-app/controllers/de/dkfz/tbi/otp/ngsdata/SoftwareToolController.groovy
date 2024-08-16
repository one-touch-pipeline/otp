/*
 * Copyright 2011-2026 The OTP authors
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

import grails.converters.JSON
import grails.validation.Validateable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class SoftwareToolController implements CheckAndCall {

    static allowedMethods = [
            list                         : "GET",
            createSoftwareTool           : "POST",
            updateSoftwareTool           : "POST",
            updateSoftwareToolIdentifier : "POST",
            createSoftwareToolIdentifier : "POST",
            changeSoftwareToolLegacyState: "POST",
    ]

    SoftwareToolService softwareToolService

    def list() {
        return [
                identifierPerSoftwareTool : softwareToolService.identifiersPerSoftwareTool(),
                softwareToolPerProgramName: softwareToolService.softwareToolsPerProgramName(),
                cmd                       : flash.cmd as CreateSoftwareToolCommand,
        ]
    }

    def changeSoftwareToolLegacyState(SoftwareToolLegacyCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "dataFields.legacy") {
            softwareToolService.changeLegacyState(cmd.softwareTool, cmd.legacy)
        }
        redirect action: 'list'
    }

    def createSoftwareTool(CreateSoftwareToolCommand cmd) {
        Errors errors = softwareToolService.createSoftwareTool(cmd.programName.trim(), cmd.programVersion.trim(), SoftwareTool.Type.BASECALLING)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "softwareTool.list.error") as String, errors)
            flash.cmd = cmd
        } else {
            flash.message = new FlashMessage(g.message(code: "softwareTool.list.success") as String)
        }
        redirect(action: 'list')
    }

    JSON updateSoftwareTool(UpdateCommand cmd) {
        return checkErrorsAndCallMethod(cmd) {
            softwareToolService.updateSoftwareTool(cmd.softwareTool, cmd.value.trim(), cmd.legacy)
            render([:] as JSON)
        }
    }

    JSON updateSoftwareToolIdentifier(UpdateIdentifierCommand cmd) {
        return checkErrorsAndCallMethod(cmd) {
            softwareToolService.updateSoftwareToolIdentifier(cmd.softwareToolIdentifier, cmd.value.trim())
            render([:] as JSON)
        }
    }

    JSON createSoftwareToolIdentifier(UpdateCommand cmd) {
        return checkErrorsAndCallMethod(cmd) {
            softwareToolService.createSoftwareToolIdentifier(cmd.softwareTool, cmd.value.trim())
            render([:] as JSON)
        }
    }
}

class UpdateCommand implements Validateable {
    SoftwareTool softwareTool
    String value
    boolean legacy

    static constraints = {
        value(blank: false)
    }
}

class UpdateIdentifierCommand implements Validateable {
    SoftwareToolIdentifier softwareToolIdentifier
    String value

    static constraints = {
        value(blank: false)
    }
}

class CreateSoftwareToolCommand implements Validateable {
    String programName
    String programVersion
}

class SoftwareToolLegacyCommand extends LegacyCommand {
    SoftwareTool softwareTool
}
