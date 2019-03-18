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
import grails.validation.Validateable

class SoftwareToolController {

    SoftwareToolService softwareToolService


    /**
     * shows all {@link SoftwareTool}s grouped by name with {@link SoftwareToolIdentifier}
     */
    Map list() {
        List dataToRender = []
        softwareToolService.uniqueSortedListOfSoftwareToolProgramNames().each { String programName ->
            List versions = []
            softwareToolService.findSoftwareToolsByProgramNameSortedAfterVersion(programName).each { SoftwareTool softwareTool ->
                List aliases = []
                softwareToolService.findSoftwareToolIdentifiersBySoftwareToolSortedAfterName(softwareTool).each { SoftwareToolIdentifier it ->
                    aliases << [id: it.id, name: it.name]
                }
                versions << [
                    id: softwareTool.id,
                    programVersion: softwareTool.programVersion,
                    softwareToolIdentifiers: aliases,
                ]
            }
            dataToRender << [
                    programName: programName,
                    versions: versions,
            ]
        }
        return [softwareTools: dataToRender]
    }

    JSON updateSoftwareTool(UpdateCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        if (softwareToolService.getSoftwareTool(cmd.id) == null) {
            Map data = [error: "requested software tool with id of " + cmd.id + " could not be found"]
            render data as JSON
            return
        }
        Map data = [success: true, softwareTool: softwareToolService.updateSoftwareTool(cmd.id, cmd.value)]
        render data as JSON
    }

    JSON updateSoftwareToolIdentifier(UpdateCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        if (softwareToolService.getSoftwareToolIdentifier(cmd.id) == null) {
            Map data = [error: "requested software tool identifier with id of " + cmd.id + " could not be found"]
            render data as JSON
            return
        }
        Map data = [success: true, softwareToolIdentifier: softwareToolService.updateSoftwareToolIdentifier(cmd.id, cmd.value)]
        render data as JSON
    }

    JSON createSoftwareToolIdentifier(UpdateCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        if (softwareToolService.getSoftwareTool(cmd.id) == null) {
            Map data = [error: "requested software tool with id of " + cmd.id + " could not be found"]
            render data as JSON
            return
        }
        SoftwareTool softwareTool = softwareToolService.getSoftwareTool(cmd.id)
        Map data = [success: true, softwareToolIdentifier: softwareToolService.createSoftwareToolIdentifier(softwareTool, cmd.value)]
        render data as JSON
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



