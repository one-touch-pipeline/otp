package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*


class SnvController {

    ProjectService projectService
    SnvService snvService

    Map results() {
        String projectName = params.projectName
        List<String> projects = projectService.getAllProjects()*.name
        if (!projectName) {
            projectName = projects.first()
        }
        Project project = projectService.getProjectByName(projectName)

        return [
            projects: projects,
            project: project.name,
        ]
    }

    Map plots(RenderFileCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        SnvCallingInstance snvCallingInstance = snvService.getSnvCallingInstance(cmd.id as long)
        if (!snvCallingInstance) {
            return [
                    error: "Sorry, invalid snv calling instance",
                    pid: "Individual not found"
            ]
        }
        if (!snvCallingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath.exists()) {
            return [
                    error: "Sorry, there is no file available",
                    pid: snvCallingInstance.individual.pid
            ]
        }
        return [
            id: cmd.id,
            pid: snvCallingInstance.individual.pid,
            error: null
        ]
    }

    def renderPDF(RenderFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        SnvCallingInstance snvCallingInstance = snvService.getSnvCallingInstance(cmd.id as long)
        if (!snvCallingInstance) {
            render status: 404
            return
        }
        File stream = snvCallingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath
        if (!stream) {
            render status: 404
            return
        }
        if (!stream.exists()) {
            render status: 404
            return
        }

        render file: stream , contentType: "application/pdf"
    }

    JSON dataTableSnvResults(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List results = snvService.getSnvCallingInstancesForProject(params.project)
        List data = results.collect { Map properties ->
            Collection<String> libPrepKitShortNames
            if (SeqTypeNames.fromSeqTypeName(properties.seqTypeName)?.isWgbs()) {
                assert properties.libPrepKit1 == null && properties.libPrepKit2 == null
                libPrepKitShortNames = SnvCallingInstance.get(properties.snvInstanceId).containedSeqTracks*.
                        libraryPreparationKit*.shortDisplayName
            } else {
                libPrepKitShortNames = [(String) properties.libPrepKit1, (String) properties.libPrepKit2]
            }
            properties.libPrepKits = libPrepKitShortNames.unique().collect { it ?: 'unknown' }.join(", <br>")
            properties.remove('libPrepKit1')
            properties.remove('libPrepKit2')
            if (properties.snvProcessingState != AnalysisProcessingStates.FINISHED) {
                properties.remove('snvInstanceId')
            }
            return properties
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }
}

class RenderFileCommand {
    String id

    static constraints = {
        id nullable: false
    }
}
