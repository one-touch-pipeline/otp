package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import java.text.SimpleDateFormat


class SnvController {

    ProjectService projectService
    AnalysisService analysisService

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

    Map plots(RenderSnvFileCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (analysisService.checkFile(cmd.snvCallingInstance)) {
            return [
                    id: cmd.snvCallingInstance.id,
                    pid: cmd.snvCallingInstance.individual.pid,
                    error: null
            ]
        }
        return [
                error: "File not found",
                pid: "no File",
        ]
    }

    def renderPDF(RenderSnvFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        File stream = analysisService.checkFile(cmd.snvCallingInstance)
        if (stream) {
            render file: stream , contentType: "application/pdf"
        } else {
            render status: 404
            return
        }
    }

    JSON dataTableSnvResults(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd hh:mm')
        List results = analysisService.getCallingInstancesForProject(SnvCallingInstance, params.project)
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
            properties.dateCreated = sdf.format(properties.dateCreated)
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

class RenderSnvFileCommand {
    SnvCallingInstance snvCallingInstance

    static constraints = {
        snvCallingInstance nullable: false
    }
}
