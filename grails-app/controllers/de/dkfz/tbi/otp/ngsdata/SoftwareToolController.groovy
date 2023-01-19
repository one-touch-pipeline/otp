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

import grails.converters.JSON
import org.springframework.security.access.annotation.Secured
import grails.validation.Validateable
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage

@Secured("hasRole('ROLE_OPERATOR')")
class SoftwareToolController {

    static allowedMethods = [
            list                        : "GET",
            createSoftwareTool          : "POST",
            updateSoftwareTool          : "POST",
            updateSoftwareToolIdentifier: "POST",
            createSoftwareToolIdentifier: "POST",
    ]

    SoftwareToolService softwareToolService

    def list() {
        return [
                identifierPerSoftwareTool : softwareToolService.identifiersPerSoftwareTool(),
                softwareToolPerProgramName: softwareToolService.softwareToolsPerProgramName(),
                cmd                       : flash.cmd as CreateSoftwareToolCommand,
        ]
    }

    def createSoftwareTool(CreateSoftwareToolCommand cmd) {
        Errors errors = softwareToolService.createSoftwareTool(cmd.programName.trim(), cmd.programVersion.trim(), SoftwareTool.Type.BASECALLING)
        if (errors) {
            flash.message = new FlashMessage(g.message(code:  "softwareTool.list.error") as String, errors)
            flash.cmd = cmd
        } else {
            flash.message = new FlashMessage(g.message(code:  "softwareTool.list.success") as String)
        }
        redirect(action: 'list')
    }

    JSON updateSoftwareTool(UpdateCommand cmd) {
        if (cmd.hasErrors()) {
            render(cmd.errors as JSON)
            return
        }
        if (softwareToolService.getSoftwareTool(cmd.id) == null) {
            Map data = [error: "requested software tool with id of ${cmd.id} could not be found"]
            render(data as JSON)
            return
        }
        Map data = [success: true, softwareTool: softwareToolService.updateSoftwareTool(cmd.id, cmd.value.trim())]
        render(data as JSON)
    }

    JSON updateSoftwareToolIdentifier(UpdateCommand cmd) {
        if (cmd.hasErrors()) {
            render(cmd.errors as JSON)
            return
        }
        if (softwareToolService.getSoftwareToolIdentifier(cmd.id) == null) {
            Map data = [error: "requested software tool identifier with id of ${cmd.id} could not be found"]
            render(data as JSON)
            return
        }
        Map data = [success: true, softwareToolIdentifier: softwareToolService.updateSoftwareToolIdentifier(cmd.id, cmd.value.trim())]
        render(data as JSON)
    }

    JSON createSoftwareToolIdentifier(UpdateCommand cmd) {
        if (cmd.hasErrors()) {
            render(cmd.errors as JSON)
            return
        }
        if (softwareToolService.getSoftwareTool(cmd.id) == null) {
            Map data = [error: "requested software tool with id of ${cmd.id} could not be found"]
            render(data as JSON)
            return
        }
        SoftwareTool softwareTool = softwareToolService.getSoftwareTool(cmd.id)
        Map data = [success: true, softwareToolIdentifier: softwareToolService.createSoftwareToolIdentifier(softwareTool, cmd.value.trim())]
        render(data as JSON)
    }
}

class UpdateCommand implements Validateable {
    Long id
    String value

    static constraints = {
        id(min: 0L)
        value(blank: false)
    }
}

class CreateSoftwareToolCommand implements Validateable {
    String programName
    String programVersion
}
