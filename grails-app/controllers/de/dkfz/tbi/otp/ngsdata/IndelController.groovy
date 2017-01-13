package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import java.text.SimpleDateFormat

class IndelController {

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

    Map plots(RenderIndelFileCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (analysisService.checkFile(cmd.indelCallingInstance)) {
            return [
                    id: cmd.indelCallingInstance.id,
                    pid: cmd.indelCallingInstance.individual.pid,
                    error: null
            ]
        }
        return [
                error: "File not found",
                pid: "no File",
        ]
    }

    def renderPDF(RenderIndelFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        File stream = analysisService.checkFile(cmd.indelCallingInstance)
        if (stream) {
            render file: stream , contentType: "application/pdf"
        } else {
            render status: 404
            return
        }
    }

    JSON dataTableIndelResults(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd hh:mm')
        List results = analysisService.getCallingInstancesForProject(IndelCallingInstance, params.project)
        List data = results.collect { Map properties ->
            Collection<String> libPrepKitShortNames
            if (SeqTypeNames.fromSeqTypeName(properties.seqTypeName)?.isWgbs()) {
                assert properties.libPrepKit1 == null && properties.libPrepKit2 == null
                libPrepKitShortNames = IndelCallingInstance.get(properties.indelInstanceId).containedSeqTracks*.
                        libraryPreparationKit*.shortDisplayName
            } else {
                libPrepKitShortNames = [(String) properties.libPrepKit1, (String) properties.libPrepKit2]
            }
            properties.libPrepKits = libPrepKitShortNames.unique().collect { it ?: 'unknown' }.join(", <br>")
            properties.remove('libPrepKit1')
            properties.remove('libPrepKit2')
            properties.dateCreated = sdf.format(properties.dateCreated)
            if (properties.indelProcessingState != AnalysisProcessingStates.FINISHED) {
                properties.remove('indelInstanceId')
            }
            return properties
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }
}

class RenderIndelFileCommand {
    IndelCallingInstance indelCallingInstance

    static constraints = {
        indelCallingInstance nullable: false
    }
}
