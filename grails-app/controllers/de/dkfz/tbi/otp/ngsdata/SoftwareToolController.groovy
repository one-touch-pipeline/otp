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

@Validateable
class UpdateCommand implements Serializable {

    Long id
    String value

    static constraints = {
        id(min: 0L)
        value(blank: false)
    }
}



